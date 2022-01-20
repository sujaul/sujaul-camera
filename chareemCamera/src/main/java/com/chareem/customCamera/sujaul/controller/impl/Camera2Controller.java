package com.chareem.customCamera.sujaul.controller.impl;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.chareem.customCamera.sujaul.configuration.CameraConfiguration;
import com.chareem.customCamera.sujaul.configuration.ConfigurationProvider;
import com.chareem.customCamera.sujaul.controller.CameraController;
import com.chareem.customCamera.sujaul.controller.view.CameraView;
import com.chareem.customCamera.sujaul.facetracker.MultiBoxTracker;
import com.chareem.customCamera.sujaul.manager.CameraManager;
import com.chareem.customCamera.sujaul.manager.impl.Camera2Manager;
import com.chareem.customCamera.sujaul.manager.listener.CameraCloseListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraOpenListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraPhotoListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraVideoListener;
import com.chareem.customCamera.sujaul.ui.view.AutoFitTextureView;
import com.chareem.customCamera.sujaul.ui.view.CameraSwitchView;
import com.chareem.customCamera.sujaul.utils.CameraHelper;
import com.chareem.customCamera.sujaul.utils.Size;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Controller implements CameraController<String>,
        CameraOpenListener<String, TextureView.SurfaceTextureListener>,
        CameraPhotoListener, CameraVideoListener, CameraCloseListener<String> {

    private final static String TAG = "Camera2Controller";

    private String currentCameraId;
    private ConfigurationProvider configurationProvider;
    private CameraManager<String, TextureView.SurfaceTextureListener> camera2Manager;
    private CameraView cameraView;
    @CameraSwitchView.CameraType private int currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR;

    private AutoFitTextureView mTextureView;
    private Size previewSize;
    private Bitmap bit;

    private File outputFile;
    private boolean isUseFaceDetection = false;

    public Camera2Controller(CameraView cameraView, ConfigurationProvider configurationProvider,
                             @CameraSwitchView.CameraType int currentCameraType,
                             boolean isUseFaceDetection) {
        this.cameraView = cameraView;
        this.configurationProvider = configurationProvider;
        this.currentCameraType = currentCameraType;
        this.isUseFaceDetection = isUseFaceDetection;
        Log.d("llllllll", isUseFaceDetection +" ookk");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("llllllll", isUseFaceDetection +" oo");
        camera2Manager = Camera2Manager.getInstance(this, isUseFaceDetection);
        camera2Manager.initializeCameraManager(configurationProvider, cameraView.getActivity());
        if (currentCameraType == CameraSwitchView.CAMERA_TYPE_REAR)
            currentCameraId = camera2Manager.getFaceBackCameraId();
        else
            currentCameraId = camera2Manager.getFaceFrontCameraId();
    }

    @Override
    public void onResume() {
        camera2Manager.openCamera(currentCameraId, this);
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
        currentCameraId = camera2Manager.getCurrentCameraId().equals(camera2Manager.getFaceFrontCameraId()) ?
                camera2Manager.getFaceBackCameraId() : camera2Manager.getFaceFrontCameraId();

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
    public void onCameraOpened(String openedCameraId, Size previewSize, TextureView.SurfaceTextureListener surfaceTextureListener) {
        this.previewSize = previewSize;
        mTextureView = new AutoFitTextureView(cameraView.getActivity(), surfaceTextureListener);
        mTextureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
        configureTransform(previewSize.getWidth(), previewSize.getHeight());
        cameraView.updateUiForMediaAction(CameraConfiguration.MEDIA_ACTION_BOTH);
        cameraView.updateCameraPreview(previewSize, mTextureView);
        cameraView.updateCameraSwitcher(camera2Manager.getNumberOfCameras());
    }

    private void configureTransform(final int viewWidth, final int viewHeight) {
        final Activity activity = cameraView.getActivity();
        if (null == mTextureView || null == previewSize || null == activity) {
            return;
        }
        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    @Override
    public void onCameraOpenError() {
        Log.e(TAG, "onCameraOpenError");
    }

    @Override
    public void onCameraClosed(String closedCameraId) {
        cameraView.releaseCameraPreview();

        camera2Manager.openCamera(currentCameraId, this);
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
    public void onSurfaceChanged(Integer sensorOrientation, Bitmap bitmap, Size photoSize) {

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
