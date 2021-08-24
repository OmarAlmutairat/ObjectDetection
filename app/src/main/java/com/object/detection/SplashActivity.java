package com.object.detection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class SplashActivity extends AppCompatActivity {
    TextToSpeech t1;
    private static final String TAG = "SplashActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);
        t1=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.e(TAG, "onInit: "+status );
                if(status == TextToSpeech.SUCCESS) {
                    t1.setLanguage(Locale.ENGLISH);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            t1.speak("Welcome back. We got you", TextToSpeech.QUEUE_FLUSH, null,null);

                        }
                    },1000);
                }else if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.ENGLISH);

                }else {
                }
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!Utils.allPermissionsGranted(this)) {
            Utils.requestRuntimePermissions(this);
        }else{
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(SplashActivity.this,LiveObjectDetectionActivity.class));
                    SplashActivity.this.finish();
                }
            },4000);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == Utils.REQUEST_CODE_PHOTO_LIBRARY
                && resultCode == Activity.RESULT_OK
                && data != null) {
            Intent intent = new Intent(this, LiveObjectDetectionActivity.class);
            intent.setData(data.getData());
            startActivity(intent);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}