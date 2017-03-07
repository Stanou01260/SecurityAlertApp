package com.imageManipulation.stanislas.security;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.opencv.android.OpenCVLoader;

public class LoginActivity extends AppCompatActivity {
    static {
        if (!OpenCVLoader.initDebug()) {
            //We couldn't load the librairies
        } else {
            // Weload
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_login);

        Button button = (Button) findViewById(R.id.button_login);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText startEdit = (EditText) findViewById(R.id.editText_Start);// get the id for edit text
                final EditText operateEdit = (EditText) findViewById(R.id.editText_Operate);// get the id for edit text
                final EditText phoneEdit = (EditText) findViewById(R.id.editText_Phone);// get the id for edit text
                CheckBox checkBoxSMS = (CheckBox) findViewById(R.id.checkBox_SMS);
                CheckBox checkBoxAlarm = (CheckBox) findViewById(R.id.checkBox_Alarm);
                int waitTime = 5;
                int operateTime = 3600;

                String phoneNumber = phoneEdit.getText().toString();

                long startTime = System.currentTimeMillis();
                boolean sendSMS = checkBoxSMS.isChecked();
                boolean playAlarm = checkBoxAlarm.isChecked();


                /*
                    GMailSender sender = new GMailSender("hzgzf2t@gmail.com", "turne310");
                try {
                    sender.sendMail("This is Subject",
                            "This is Body",
                            "hzgzf2t@gmail.com",
                            "hzgzf2t@gmail.com");
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
                boolean requiredField = false;
                if (phoneNumber.equals("") && sendSMS) {
                    phoneEdit.setHint("Write phone number");
                    phoneEdit.setBackgroundColor(Color.RED);
                    requiredField = true;
                }

                if (operateEdit.getText().toString().equals("")) {
                    phoneEdit.setHint("Required");

                    operateEdit.setBackgroundColor(Color.RED);
                    requiredField = true;
                } else {
                    operateTime = 3600 * Integer.parseInt(operateEdit.getText().toString());
                }
                if (startEdit.getText().toString().equals("")) {
                    startEdit.setBackgroundColor(Color.RED);
                    startEdit.setHint("Required");

                    requiredField = true;
                } else {
                    waitTime = Integer.parseInt(startEdit.getText().toString());

                }

                if (requiredField == false) {
                    Intent intent = new Intent(getApplicationContext(), ImageManipulation.class);
                    intent.putExtra("waitTime", waitTime);
                    intent.putExtra("operateTime", operateTime);
                    intent.putExtra("phoneNumber", phoneNumber);
                    intent.putExtra("startTime", startTime);
                    intent.putExtra("sendSMS", sendSMS);
                    intent.putExtra("playAlarm", playAlarm);
                    startActivity(intent);
                }
            }
        });


        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyhavePermission()) {
                requestForSpecificPermission();
            }
        }

    }

    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SEND_SMS, Manifest.permission.INTERNET}, 101);
    }

    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
}