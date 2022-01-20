package com.chareem.customCamera.sujaul.manager.listener;


import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.chareem.customCamera.sujaul.utils.Size;

import java.io.File;

/**
 * Created by Arpit Gandhi on 8/14/16.
 */
public interface CameraPhotoListener {
    void onPhotoTaken(File photoFile);

    void onPhotoTakeError();

    void onSurfaceChanged(Integer sensorOrientation, @Nullable Bitmap bitmap, Size photoSize);
}
