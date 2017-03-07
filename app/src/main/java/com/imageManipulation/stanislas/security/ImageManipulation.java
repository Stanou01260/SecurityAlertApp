package com.imageManipulation.stanislas.security;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageManipulation extends Activity implements OnTouchListener, CvCameraViewListener2 {
    static {
        OpenCVLoader.initDebug();
    }

    final int NBFRAMESSKIPPED = 1;
    final int ALERTTRESHOLD = 10;
    final int NBFRAMEBETWEENALERT = 500;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Button button;
    private String folderDirectory;
    private Mat background;
    private int frameCount = 0;
    private Mat lastOutput;
    private int lastText = -10000000;
    private MediaPlayer mp;

    int waitTime;
    int operateTime;
    String phoneNumber;
    long startTime;
    boolean sendSMS;
    boolean playAlarm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_manipulation);

        WindowManager.LayoutParams params = this.getWindow().getAttributes();
        params.screenBrightness = 0;
        getWindow().setAttributes(params);

        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "SECURITY");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        mp = MediaPlayer.create(getApplicationContext(), R.raw.alarm);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setOnTouchListener(ImageManipulation.this);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView();

        /*
        button = (Button) findViewById(R.id.button_save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }
        });*/

        waitTime = getIntent().getExtras().getInt("waitTime");
        operateTime = getIntent().getExtras().getInt("operateTime");
        phoneNumber = getIntent().getExtras().getString("phoneNumber");
        startTime = getIntent().getExtras().getLong("startTime");
        sendSMS = getIntent().getExtras().getBoolean("sendSMS");
        playAlarm = getIntent().getExtras().getBoolean("playAlarm");


    }

    public void onCameraViewStarted(int width, int height) {
        //We set the folder name:
        SimpleDateFormat stfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strDate = stfDate.format(now);
        folderDirectory = Environment.getExternalStorageDirectory() + File.separator + "SECURITY" + File.separator + strDate;

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat inputImage = inputFrame.rgba();
        Mat inputImageGray = inputFrame.gray();
        boolean stopProcess = false;
        long cTime = System.currentTimeMillis();
        long dTime = (cTime - startTime) / 1000;

        if (dTime < waitTime) {
            Imgproc.putText(inputImage, "Start in " + (waitTime - dTime), new Point(1, 50), Core.FONT_HERSHEY_PLAIN, 4, new Scalar(255, 255, 255));
            stopProcess = true;
        }

        if (dTime > operateTime) {
            stopProcess = true;
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }

        if (stopProcess == false) {
            if (frameCount % NBFRAMESSKIPPED == 0) {
                lastOutput = imgProcess(inputImage, inputImageGray).clone();
            }
            frameCount++;
            return (lastOutput);


        } else {
            return (inputImage);
        }


    }

    public void saveImageToFile(Mat image, String name, int num) {
        File folder = new File(folderDirectory);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String fileName = folderDirectory + File.separator + num + name + ".jpg";
        Mat bgra = new Mat();
        Imgproc.cvtColor(image, bgra, Imgproc.COLOR_RGBA2BGRA);
        Boolean bool = Imgcodecs.imwrite(fileName, bgra);
    }

    public void onCameraViewStopped() {

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        return false; // don't need subsequent touch events
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public Mat imgProcess(Mat inputImage, Mat inputImageGray) {
        // Store background if it is the first frame:
        if (background == null) {
            background = inputImageGray.clone();
        }

        Mat grayWithoutBackground = new Mat();
        // On enlève le background:
        Core.absdiff(inputImageGray, background, grayWithoutBackground);

        // We threshold the difference to create a mask:
        Mat backgroundMask = new Mat();
        Imgproc.threshold(grayWithoutBackground, backgroundMask, 25, 255, Imgproc.THRESH_BINARY);
        Mat morphElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20));
        Imgproc.erode(backgroundMask, backgroundMask, morphElement);
        Imgproc.dilate(backgroundMask, backgroundMask, morphElement);


        //We calculate if there is something going on:
        Size dsSize = backgroundMask.size();
        int dsWidth = (int) dsSize.width;
        int dsHeight = (int) dsSize.height;
        double movement = 100 * Core.countNonZero(backgroundMask) / (dsWidth * dsHeight);
        Mat mHierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(backgroundMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        SimpleDateFormat stfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strDate = stfDate.format(now);
        if (movement > ALERTTRESHOLD) {
            for (int i = 0; i < contours.size(); i++) {
                Imgproc.drawContours(inputImage, contours, i, new Scalar(255, 255, 255));
            }
            Imgproc.putText(inputImage, "SOMETHING IS GOING ON", new Point(1, 50), Core.FONT_HERSHEY_PLAIN, 4, new Scalar(255, 0, 0));
            Imgproc.putText(inputImage, strDate, new Point(1, 100), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 0, 0));


            if (sendSMS && (frameCount - lastText > NBFRAMEBETWEENALERT)) {

                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(phoneNumber, null, "Hey, be careful, something is happening at your house\n" + strDate + "\nThe Security Alert Team", null, null);
                lastText = frameCount;
            }

            if (playAlarm) {
                mp.start();
            }
            saveImageToFile(inputImage, "SecurityAlert", frameCount);

        } else {

            Imgproc.putText(inputImage, "NOTHING HAPPENNING ", new Point(1, 50), Core.FONT_HERSHEY_PLAIN, 4, new Scalar(0, 255, 0));
            Imgproc.putText(inputImage, strDate, new Point(1, 100), Core.FONT_HERSHEY_PLAIN, 4, new Scalar(0, 255, 0));
            //saveImageToFile(inputImage,"SecurityAlert",frameCount);

        }
        //Save background for the next frame
        background = inputImageGray.clone();
        return (inputImage);
    }
}