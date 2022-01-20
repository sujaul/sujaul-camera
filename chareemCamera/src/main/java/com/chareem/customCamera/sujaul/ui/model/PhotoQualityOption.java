package com.chareem.customCamera.sujaul.ui.model;

import com.chareem.customCamera.sujaul.configuration.CameraConfiguration;
import com.chareem.customCamera.sujaul.utils.Size;

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

public class PhotoQualityOption implements CharSequence {

    @CameraConfiguration.MediaQuality
    private int mediaQuality;
    private String title;

    public PhotoQualityOption(@CameraConfiguration.MediaQuality int mediaQuality, Size size) {
        this.mediaQuality = mediaQuality;

        title = size.getWidth() + " x " + size.getHeight();
    }

    @CameraConfiguration.MediaQuality
    public int getMediaQuality() {
        return mediaQuality;
    }

    @Override
    public int length() {
        return title.length();
    }

    @Override
    public char charAt(int index) {
        return title.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return title.subSequence(start, end);
    }

    @Override
    public String toString() {
        return title;
    }
}
