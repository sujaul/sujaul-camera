package com.chareem.customCamera.sujaul.manager;

import android.content.Context;

import com.chareem.customCamera.sujaul.configuration.CameraConfiguration;
import com.chareem.customCamera.sujaul.configuration.ConfigurationProvider;
import com.chareem.customCamera.sujaul.manager.listener.CameraCloseListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraOpenListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraPhotoListener;
import com.chareem.customCamera.sujaul.manager.listener.CameraVideoListener;
import com.chareem.customCamera.sujaul.utils.Size;

import java.io.File;

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
public interface CameraManager<CameraId, SurfaceListener> {

    void initializeCameraManager(ConfigurationProvider configurationProvider, Context context);

    void openCamera(CameraId cameraId, CameraOpenListener<CameraId, SurfaceListener> cameraOpenListener);

    void closeCamera(CameraCloseListener<CameraId> cameraCloseListener);

    void takePhoto(File photoFile, CameraPhotoListener cameraPhotoListener);

    void startVideoRecord(File videoFile, CameraVideoListener cameraVideoListener);

    Size getPhotoSizeForQuality(@CameraConfiguration.MediaQuality int mediaQuality);

    void setFlashMode(@CameraConfiguration.FlashMode int flashMode);

    void stopVideoRecord();

    void releaseCameraManager();

    CameraId getCurrentCameraId();

    CameraId getFaceFrontCameraId();

    CameraId getFaceBackCameraId();

    int getNumberOfCameras();

    int getFaceFrontCameraOrientation();

    int getFaceBackCameraOrientation();

    boolean isVideoRecording();
}
