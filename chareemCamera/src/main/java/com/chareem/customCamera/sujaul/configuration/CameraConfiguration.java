package com.chareem.customCamera.sujaul.configuration;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Arpit Gandhi on 7/6/16.
 */
public final class CameraConfiguration {

    public static final int MEDIA_QUALITY_AUTO = 10;
    public static final int MEDIA_QUALITY_LOWEST = 15;
    public static final int MEDIA_QUALITY_LOW = 11;
    public static final int MEDIA_QUALITY_MEDIUM = 12;
    public static final int MEDIA_QUALITY_HIGH = 13;
    public static final int MEDIA_QUALITY_HIGHEST = 14;

    public static final int MEDIA_ACTION_VIDEO = 100;
    public static final int MEDIA_ACTION_PHOTO = 101;
    public static final int MEDIA_ACTION_BOTH = 102;

    public static final int CAMERA_FACE_FRONT = 0x6;
    public static final int CAMERA_FACE_REAR = 0x7;

    public static final int SENSOR_POSITION_UP = 90;
    public static final int SENSOR_POSITION_LEFT = 0;
    public static final int SENSOR_POSITION_RIGHT = 180;
    public static final int SENSOR_POSITION_UNSPECIFIED = -1;
    public static final int ORIENTATION_PORTRAIT = 0x111;
    public static final int ORIENTATION_LANDSCAPE = 0x222;
    public static final int FLASH_MODE_ON = 1;
    public static final int FLASH_MODE_OFF = 2;
    public static final int FLASH_MODE_AUTO = 3;
    static final int DISPLAY_ROTATION_0 = 0;
    static final int DISPLAY_ROTATION_90 = 90;
    static final int DISPLAY_ROTATION_180 = 180;
    static final int DISPLAY_ROTATION_270 = 270;

    public interface Arguments {
        String MEDIA_ACTION = "com.chareem.chareemCamera.media_action";
        String MEDIA_QUALITY = "com.chareem.chareemCamera.camera_media_quality";
        String VIDEO_DURATION = "com.chareem.chareemCamera.video_duration";
        String MINIMUM_VIDEO_DURATION = "com.chareem.chareemCamera.minimum.video_duration";
        String VIDEO_FILE_SIZE = "com.chareem.chareemCamera.camera_video_file_size";
        String FLASH_MODE = "com.chareem.chareemCamera.camera_flash_mode";
        String SHOW_PICKER = "com.chareem.chareemCamera.show_picker";
        String SHOW_SETTING = "com.chareem.chareemCamera.show_setting";
        String SHOW_LOCATION = "com.chareem.chareemCamera.show_location";
        String MOCK_LOCATION = "com.chareem.chareemCamera.mock_location";
        String IS_PERMISSIONS_GRANTED = "com.chareem.chareemCamera.permissions_granted";
        String ENABLE_CROP = "com.chareem.chareemCamera.enable_crop";
        String AUTO_RECORD = "com.chareem.chareemCamera.auto_record";
        String DEVAULT_LAT = "com.chareem.chareemCamera.devault_lat";
        String DEVAULT_LON = "com.chareem.chareemCamera.devault_lon";
        String CAMERA_FACING_TYPE = "com.chareem.chareemCamera.camera_facing_type";
        String FACE_DETECTION = "com.chareem.chareemCamera.face_detection";
    }

    @IntDef({MEDIA_QUALITY_AUTO, MEDIA_QUALITY_LOWEST, MEDIA_QUALITY_LOW, MEDIA_QUALITY_MEDIUM, MEDIA_QUALITY_HIGH, MEDIA_QUALITY_HIGHEST})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaQuality {
    }

    @IntDef({MEDIA_ACTION_VIDEO, MEDIA_ACTION_PHOTO, MEDIA_ACTION_BOTH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaAction {
    }

    @IntDef({CAMERA_FACE_FRONT, CAMERA_FACE_REAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraFace {
    }

    @IntDef({SENSOR_POSITION_UP, SENSOR_POSITION_LEFT, SENSOR_POSITION_RIGHT, SENSOR_POSITION_UNSPECIFIED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SensorPosition {
    }

    @IntDef({DISPLAY_ROTATION_0, DISPLAY_ROTATION_90, DISPLAY_ROTATION_180, DISPLAY_ROTATION_270})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DisplayRotation {
    }

    @IntDef({ORIENTATION_PORTRAIT, ORIENTATION_LANDSCAPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeviceDefaultOrientation {
    }

    @IntDef({FLASH_MODE_ON, FLASH_MODE_OFF, FLASH_MODE_AUTO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FlashMode {
    }
}
