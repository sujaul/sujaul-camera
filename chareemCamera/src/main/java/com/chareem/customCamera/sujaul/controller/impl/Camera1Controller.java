package com.chareem.customCamera.sujaul.controller.impl;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.chareem.customCamera.sujaul.configuration.CameraConfiguration;
import com.chareem.customCamera.sujaul.configuration.ConfigurationProvider;
import com.chareem.customCamera.sujaul.controller.CameraController;
import com.chareem.customCamera.sujaul.controller.view.CameraView;
import com.chareem.customCamera.sujaul.manager.CameraManager;
import com.chareem.customCamera.sujaul.manager.impl.Camera1Manager;
import com.chareem.customCamera.sujaul.manager.listener.CameraCloseListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraOpenListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraPhotoListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraVideoListener;
import com.chareem.customCamera.sujaul.ui.BaseChareemActivity;
import com.chareem.customCamera.sujaul.ui.ChareemCameraActivity;
import com.chareem.customCamera.sujaul.ui.view.AutoFitSurfaceView;
import com.chareem.customCamera.sujaul.ui.view.CameraSwitchView;
import com.chareem.customCamera.sujaul.utils.CameraHelper;
import com.chareem.customCamera.sujaul.utils.Size;

import java.io.File;

/**
 * Created by Arpit Gandhi on 7/7/16.
 */

@SuppressWarnings("deprecation")
public class Camera1Controller implements CameraController<Integer>,
        CameraOpenListener<Integer, SurfaceHolder.Callback>, CameraPhotoListener, CameraCloseListener<Integer>,
        CameraVideoListener {

    private final static String TAG = "Camera1Controller";

    private Integer currentCameraId;
    private ConfigurationProvider configurationProvider;
    private CameraManager<Integer, SurfaceHolder.Callback> cameraManager;
    private CameraView cameraView;
    @CameraSwitchView.CameraType private int currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR;

    private File outputFile;

    private Size previewSize;
    private Bitmap bit;
    private boolean isUseFaceDetection = false;

    public Camera1Controller(CameraView cameraView, ConfigurationProvider configurationProvider,
                             @CameraSwitchView.CameraType int currentCameraType, boolean isUseFaceDetection) {
        this.cameraView = cameraView;
        this.configurationProvider = configurationProvider;
        this.currentCameraType = currentCameraType;
        this.isUseFaceDetection = isUseFaceDetection;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        cameraManager = Camera1Manager.getInstance(this, cameraView.getActivity(), isUseFaceDetection);
        cameraManager.initializeCameraManager(configurationProvider, cameraView.getActivity());
        if (currentCameraType == CameraSwitchView.CAMERA_TYPE_REAR)
            currentCameraId = cameraManager.getFaceBackCameraId();
        else
            currentCameraId = cameraManager.getFaceFrontCameraId();
    }

    @Override
    public void onResume() {
        cameraManager.openCamera(currentCameraId, this);
    }

    @Override
    public void onPause() {
        cameraManager.closeCamera(null);
    }

    @Override
    public void onDestroy() {
        cameraManager.releaseCameraManager();
    }

    @Override
    public void takePhoto() {
        outputFile = CameraHelper.getOutputMediaFile(cameraView.getActivity(), CameraConfiguration.MEDIA_ACTION_PHOTO);
        cameraManager.takePhoto(outputFile, this);
    }

    @Override
    public void startVideoRecord() {
        outputFile = CameraHelper.getOutputMediaFile(cameraView.getActivity(), CameraConfiguration.MEDIA_ACTION_VIDEO);
        cameraManager.startVideoRecord(outputFile, this);
    }

    @Override
    public void setFlashMode(@CameraConfiguration.FlashMode int flashMode) {
        cameraManager.setFlashMode(flashMode);
    }

    @Override
    public void stopVideoRecord() {
        cameraManager.stopVideoRecord();
    }

    @Override
    public boolean isVideoRecording() {
        return cameraManager.isVideoRecording();
    }

    @Override
    public void switchCamera(@CameraConfiguration.CameraFace final int cameraFace) {
        currentCameraId = cameraManager.getCurrentCameraId().equals(cameraManager.getFaceFrontCameraId()) ?
                cameraManager.getFaceBackCameraId() : cameraManager.getFaceFrontCameraId();

        cameraManager.closeCamera(this);
    }

    @Override
    public void switchQuality() {
        cameraManager.closeCamera(this);
    }

    @Override
    public int getNumberOfCameras() {
        return cameraManager.getNumberOfCameras();
    }

    @Override
    public int getMediaAction() {
        return configurationProvider.getMediaAction();
    }

    @Override
    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public Integer getCurrentCameraId() {
        return currentCameraId;
    }


    @Override
    public void onCameraOpened(Integer cameraId, Size previewSize, SurfaceHolder.Callback surfaceCallback) {
        this.previewSize = previewSize;
        cameraView.updateUiForMediaAction(configurationProvider.getMediaAction());
        cameraView.updateCameraPreview(previewSize, new AutoFitSurfaceView(cameraView.getActivity(), surfaceCallback));
        cameraView.updateCameraSwitcher(getNumberOfCameras());
    }

    @Override
    public void onCameraOpenError() {
        Log.e(TAG, "onCameraOpenError");
    }

    @Override
    public void onCameraClosed(Integer closedCameraId) {
        cameraView.releaseCameraPreview();

        cameraManager.openCamera(currentCameraId, this);
    }

    @Override
    public void onPhotoTaken(File photoFile) {
        cameraView.onPhotoTaken();
    }

    @Override
    public void onPhotoTakeError() {
        Toast.makeText(cameraView.getActivity(),"Capture photo failed, please check your setting directory and storage space", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSurfaceChanged(Integer sensorOrientation, @Nullable Bitmap bitmap, Size photoSize) {

    }

    @Override
    public void onVideoRecordStarted(Size videoSize) {
        cameraView.onVideoRecordStart(videoSize.getWidth(), videoSize.getHeight());
    }

    @Override
    public void onVideoRecordStopped(File videoFile) {
        cameraView.onVideoRecordStop();
    }

    @Override
    public void onVideoRecordError() {
        Toast.makeText(cameraView.getActivity(),"Record video failed, please check your setting directory and storage space", Toast.LENGTH_LONG).show();
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }
}
