package com.chareem.customCamera.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chareem.customCamera.sujaul.ChareemCamera;
import com.chareem.customCamera.sujaul.configuration.CameraConfiguration;
import com.chareem.customCamera.sujaul.ui.model.Media;
import com.chareem.customCamera.sujaul.ui.view.CameraSwitchView;

/**
 * Sample for Sandrios Camera library
 * Created by Arpit Gandhi on 11/8/16.
 */

public class MainActivity extends AppCompatActivity {

    private AppCompatActivity activity;
    private int facing = CameraSwitchView.CAMERA_TYPE_REAR;
    private String latitude = "";
    private String longitude = "";
    private String capture_path = null;
    private String image_name = null;
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.withPicker:
                    ChareemCamera
                            .with()
                            .setShowPicker(true)
                            .setVideoFileSize(20)
                            .setMediaAction(CameraConfiguration.MEDIA_ACTION_BOTH)
                            .enableImageCropping(true)
                            .launchCamera(activity);
                    break;
                case R.id.withoutPicker:
                    ChareemCamera
                            .with()
                            .setShowPicker(false)
                            .setShowSettings(false)
                            .setMediaAction(CameraConfiguration.MEDIA_ACTION_PHOTO)
                            .enableImageCropping(false)
                            .setTimeStamp(true)
                            .setMockDetection(true)
                            .setCameraFacing(CameraSwitchView.CAMERA_TYPE_REAR)
                            .launchCamera(activity);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);
        activity = this;

        findViewById(R.id.withPicker).setOnClickListener(onClickListener);
        findViewById(R.id.withoutPicker).setOnClickListener(onClickListener);

        if (getIntent().hasExtra("capture_path")){
            launchCameraDirectly();
        } else {
            findViewById(R.id.withPicker).setVisibility(View.VISIBLE);
            findViewById(R.id.withoutPicker).setVisibility(View.VISIBLE);
        }
    }

    private void launchCameraDirectly(){
        if (getIntent().hasExtra("facing")) {
            try {
                facing = getIntent().getIntExtra("facing", CameraSwitchView.CAMERA_TYPE_REAR);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        if (getIntent().hasExtra("latitude")) {
            try {
                latitude = getIntent().getStringExtra("latitude");
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        if (getIntent().hasExtra("longitude")) {
            try {
                longitude = getIntent().getStringExtra("longitude");
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        if (getIntent().hasExtra("capture_path")) {
            try {
                capture_path = getIntent().getStringExtra("capture_path");
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        if (getIntent().hasExtra("image_name")) {
            try {
                image_name = getIntent().getStringExtra("image_name");
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        ChareemCamera
                .with()
                .setShowPicker(false)
                .setShowSettings(false)
                .setMediaAction(CameraConfiguration.MEDIA_ACTION_PHOTO)
                .enableImageCropping(false)
                .setDevaultPosition(latitude, longitude)
                .setTimeStamp(true)
                .setMockDetection(true)
                //.setFileDirectory(capture_path)
                .setFileName(image_name)
                .setCameraFacing((facing == CameraSwitchView.CAMERA_TYPE_REAR) ?
                        CameraSwitchView.CAMERA_TYPE_REAR : CameraSwitchView.CAMERA_TYPE_FRONT
                )
                .launchCamera(activity);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (getIntent().hasExtra("capture_path")){
            if(
                    resultCode == Activity.RESULT_OK && requestCode == ChareemCamera.RESULT_CODE && data != null
            ) {
                if (data.getSerializableExtra(ChareemCamera.MEDIA) instanceof Media) {
                    setResult(Activity.RESULT_OK);
                    finish();
                } else finish();
            } else if (requestCode == ChareemCamera.RESULT_CODE){
                finish();
            }
        } else {
            if (resultCode == Activity.RESULT_OK
                    && requestCode == ChareemCamera.RESULT_CODE
                    && data != null) {
                if (data.getSerializableExtra(ChareemCamera.MEDIA) instanceof Media) {
                    Media media = (Media) data.getSerializableExtra(ChareemCamera.MEDIA);

                    Log.e("File", "" + media.getPath());
                    Log.e("Type", "" + media.getType());
                    Toast.makeText(getApplicationContext(), "Media captured.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
