package com.chareem.customCamera.sujaul.controller.impl;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
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
import com.chareem.customCamera.sujaul.ui.view.AutoFitSurfaceView;
import com.chareem.customCamera.sujaul.ui.view.CameraSwitchView;
import com.chareem.customCamera.sujaul.utils.CameraHelper;
import com.chareem.customCamera.sujaul.utils.Size;

import java.io.File;

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
@TargetApi(Build.VERSION_CODES.N)
public class Camera2ControllerAPI24 implements CameraController<String>,
        CameraOpenListener<Integer, SurfaceHolder.Callback>,
        CameraPhotoListener, CameraVideoListener, CameraCloseListener<Integer> {

    private final static String TAG = "Camera2Controller";

    private String currentCameraId;
    private ConfigurationProvider configurationProvider;
    private CameraManager<Integer, SurfaceHolder.Callback> camera2Manager;
    private CameraView cameraView;
    @CameraSwitchView.CameraType private int currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR;

    private File outputFile;

    private Size previewSize;
    private Bitmap bit;

    private boolean isUseFaceDetection = false;

    public Camera2ControllerAPI24(CameraView cameraView, ConfigurationProvider configurationProvider,
                                  @CameraSwitchView.CameraType int currentCameraType, boolean isUseFaceDetection) {
        this.cameraView = cameraView;
        this.configurationProvider = configurationProvider;
        this.currentCameraType = currentCameraType;
        this.isUseFaceDetection = isUseFaceDetection;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        camera2Manager = Camera1Manager.getInstance(this, cameraView.getActivity(), isUseFaceDetection);
        camera2Manager.initializeCameraManager(configurationProvider, cameraView.getActivity());
        if (currentCameraType == CameraSwitchView.CAMERA_TYPE_REAR)
            currentCameraId = String.valueOf(camera2Manager.getFaceBackCameraId());
        else
            currentCameraId = String.valueOf(camera2Manager.getFaceFrontCameraId());
    }

    @Override
    public void onResume() {
        camera2Manager.openCamera(Integer.valueOf(currentCameraId), this);
    }

    @Override
    public void onPause() {
        camera2Manager.closeCamera(null);
        cameraView.releaseCameraPreview();
    }

    @Override
    public void onDestroy() {
        camera2Manager.releaseCameraManager();
    }

    @Override
    public void takePhoto() {
        outputFile = CameraHelper.getOutputMediaFile(cameraView.getActivity(), CameraConfiguration.MEDIA_ACTION_PHOTO);
        camera2Manager.takePhoto(outputFile, this);
    }

    @Override
    public void startVideoRecord() {
        outputFile = CameraHelper.getOutputMediaFile(cameraView.getActivity(), CameraConfiguration.MEDIA_ACTION_VIDEO);
        camera2Manager.startVideoRecord(outputFile, this);
    }

    @Override
    public void stopVideoRecord() {
        camera2Manager.stopVideoRecord();
    }

    @Override
    public boolean isVideoRecording() {
        return camera2Manager.isVideoRecording();
    }

    @Override
    public void switchCamera(final @CameraConfiguration.CameraFace int cameraFace) {
        currentCameraId = String.valueOf(camera2Manager.getCurrentCameraId().equals(camera2Manager.getFaceFrontCameraId()) ?
                camera2Manager.getFaceBackCameraId() : camera2Manager.getFaceFrontCameraId());

        camera2Manager.closeCamera(this);
    }

    @Override
    public void setFlashMode(@CameraConfiguration.FlashMode int flashMode) {
        camera2Manager.setFlashMode(flashMode);
    }

    @Override
    public void switchQuality() {
        camera2Manager.closeCamera(this);
    }

    @Override
    public int getNumberOfCameras() {
        return camera2Manager.getNumberOfCameras();
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
    public String getCurrentCameraId() {
        return currentCameraId;
    }

    @Override
    public void onCameraOpened(Integer openedCameraId, Size previewSize, SurfaceHolder.Callback surfaceTextureListener) {
        this.previewSize = previewSize;
        cameraView.updateUiForMediaAction(CameraConfiguration.MEDIA_ACTION_BOTH);
        cameraView.updateCameraPreview(previewSize, new AutoFitSurfaceView(cameraView.getActivity(), surfaceTextureListener));
        cameraView.updateCameraSwitcher(camera2Manager.getNumberOfCameras());
    }

    @Override
    public void onCameraOpenError() {
        Log.e(TAG, "onCameraOpenError");
    }

    @Override
    public void onCameraClosed(Integer closedCameraId) {
        cameraView.releaseCameraPreview();

        camera2Manager.openCamera(Integer.valueOf(currentCameraId), this);
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
        return camera2Manager;
    }
}
