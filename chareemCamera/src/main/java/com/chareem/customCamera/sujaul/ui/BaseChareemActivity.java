package com.chareem.customCamera.sujaul.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.chareem.customCamera.sujaul.facetracker.MultiBoxTracker;
import com.chareem.customCamera.sujaul.facetracker.SimilarityClassifier;
import com.chareem.customCamera.sujaul.facetracker.env.ImageUtils;
import com.chareem.customCamera.sujaul.ui.view.CameraSoundView;
import com.chareem.customCamera.sujaul.utils.FusedLocation;
import com.chareem.customCamera.sujaul.utils.SimpleLocation;
import com.chareem.customCamera.R;
import com.chareem.customCamera.sujaul.ChareemCamera;
import com.chareem.customCamera.sujaul.configuration.CameraConfiguration;
import com.chareem.customCamera.sujaul.ui.model.Media;
import com.chareem.customCamera.sujaul.ui.model.PhotoQualityOption;
import com.chareem.customCamera.sujaul.ui.model.VideoQualityOption;
import com.chareem.customCamera.sujaul.ui.preview.PreviewActivity;
import com.chareem.customCamera.sujaul.ui.view.CameraControlPanel;
import com.chareem.customCamera.sujaul.ui.view.CameraSwitchView;
import com.chareem.customCamera.sujaul.ui.view.FlashSwitchView;
import com.chareem.customCamera.sujaul.ui.view.MediaActionSwitchView;
import com.chareem.customCamera.sujaul.ui.view.RecordButton;
import com.chareem.customCamera.sujaul.utils.RecyclerItemClickListener;
import com.chareem.customCamera.sujaul.utils.Size;
import com.chareem.customCamera.sujaul.utils.Utils;
import com.google.android.gms.tasks.OnFailureListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Arpit Gandhi on 12/1/16.
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class BaseChareemActivity<CameraId> extends ChareemCameraActivity<CameraId>
        implements
        RecordButton.RecordButtonListener,
        FlashSwitchView.FlashModeSwitchListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener,
        CameraSwitchView.OnCameraTypeChangeListener,
        CameraControlPanel.SettingsClickListener,
        RecyclerItemClickListener.OnClickListener,
        CameraSoundView.OnSoundTypeChangeListener,
        CameraControlPanel.TrackerOnDrawListener{

    public static final int ACTION_CONFIRM = 900;
    public static final int ACTION_RETAKE = 901;
    public static final int ACTION_CANCEL = 902;
    protected static final int REQUEST_PREVIEW_CODE = 1001;
    @CameraConfiguration.MediaAction
    protected int mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH;
    @CameraConfiguration.MediaQuality
    protected int mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST;
    @CameraConfiguration.MediaQuality
    protected int passedMediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST;
    protected CharSequence[] videoQualities;
    protected CharSequence[] photoQualities;
    protected boolean enableImageCrop = false;
    protected int videoDuration = -1;
    protected long videoFileSize = -1;
    protected boolean autoRecord = false;
    protected int minimumVideoDuration = -1;
    protected boolean showPicker = true;
    protected boolean showSetting = true;
    protected boolean isUseTimeStamp = false;
    protected boolean isUseMockDetection = false;
    @MediaActionSwitchView.MediaActionState
    protected int currentMediaActionState;
    @CameraConfiguration.MediaQuality
    protected int newQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST;
    @CameraConfiguration.FlashMode
    protected int flashMode = CameraConfiguration.FLASH_MODE_AUTO;
    private List<Media> mediaList = new ArrayList<>();
    private CameraControlPanel cameraControlPanel;
    private AlertDialog settingsDialog;
    private FusedLocation fusedLocation = null;
    private SimpleLocation gps = null;
    private String currLat = "";
    private String currLon = "";
    private String addressText = "";

    private MultiBoxTracker tracker;
    private boolean computingDetection = false;

    private int previewWidth;
    private int previewHeight;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap portraitBmp = null;
    private Bitmap faceBmp = null;
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private Integer sensorOrientation;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private static final boolean MAINTAIN_ASPECT = false;

    private boolean isUseFaceDetection = false;
    private boolean isFaceAvailable = true;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getExtras().containsKey(CameraConfiguration.Arguments.IS_PERMISSIONS_GRANTED)){
            boolean isPerMissionsGranted = getIntent().getExtras().getBoolean(CameraConfiguration.Arguments.IS_PERMISSIONS_GRANTED);
            if (!isPerMissionsGranted) {
                Toast.makeText(this, "Pleas grant all permission first!!!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        fetchMediaList();
    }

    @Override
    protected void onProcessBundle(Bundle savedInstanceState) {
        super.onProcessBundle(savedInstanceState);
        extractConfiguration(getIntent().getExtras());
        currentMediaActionState = mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO ?
                MediaActionSwitchView.ACTION_VIDEO : MediaActionSwitchView.ACTION_PHOTO;
    }

    @Override
    protected void onCameraControllerReady() {
        super.onCameraControllerReady();

        videoQualities = getVideoQualityOptions();
        photoQualities = getPhotoQualityOptions();
    }

    @Override
    protected void onDestroy(){
        if (gps != null) gps.endUpdates();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isUseTimeStamp){
            if (gps == null) {
                gps = new  SimpleLocation(getApplication(), false, false, 1000, false);
                if (gps.hasLocationEnabled()){
                    initLocation();
                } else  gps.openSettings(getActivity());
            }
            cameraControlPanel.showLocation(true);
        }

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);
        cameraControlPanel.showPicker(showPicker);
        cameraControlPanel.showSetting(showSetting);
        cameraControlPanel.setCameraFacingView(currentCameraType);
    }

    @Override
    protected void onPause() {
        super.onPause();

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);
    }

    private void extractConfiguration(Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(CameraConfiguration.Arguments.MEDIA_ACTION)) {
                switch (bundle.getInt(CameraConfiguration.Arguments.MEDIA_ACTION)) {
                    case CameraConfiguration.MEDIA_ACTION_PHOTO:
                        mediaAction = CameraConfiguration.MEDIA_ACTION_PHOTO;
                        break;
                    case CameraConfiguration.MEDIA_ACTION_VIDEO:
                        mediaAction = CameraConfiguration.MEDIA_ACTION_VIDEO;
                        break;
                    default:
                        mediaAction = CameraConfiguration.MEDIA_ACTION_BOTH;
                        break;
                }
            }

            if (bundle.containsKey(CameraConfiguration.Arguments.MEDIA_QUALITY)) {
                switch (bundle.getInt(CameraConfiguration.Arguments.MEDIA_QUALITY)) {
                    case CameraConfiguration.MEDIA_QUALITY_AUTO:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_AUTO;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_HIGHEST:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGHEST;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_HIGH:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_HIGH;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_MEDIUM:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_MEDIUM;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_LOW:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_LOW;
                        break;
                    case CameraConfiguration.MEDIA_QUALITY_LOWEST:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_LOWEST;
                        break;
                    default:
                        mediaQuality = CameraConfiguration.MEDIA_QUALITY_MEDIUM;
                        break;
                }
                passedMediaQuality = mediaQuality;
            }

            if (bundle.containsKey(CameraConfiguration.Arguments.VIDEO_DURATION))
                videoDuration = bundle.getInt(CameraConfiguration.Arguments.VIDEO_DURATION);

            if (bundle.containsKey(CameraConfiguration.Arguments.VIDEO_FILE_SIZE))
                videoFileSize = bundle.getLong(CameraConfiguration.Arguments.VIDEO_FILE_SIZE);

            if (bundle.containsKey(CameraConfiguration.Arguments.MINIMUM_VIDEO_DURATION))
                minimumVideoDuration = bundle.getInt(CameraConfiguration.Arguments.MINIMUM_VIDEO_DURATION);

            if (bundle.containsKey(CameraConfiguration.Arguments.SHOW_PICKER))
                showPicker = bundle.getBoolean(CameraConfiguration.Arguments.SHOW_PICKER);

            if (bundle.containsKey(CameraConfiguration.Arguments.SHOW_SETTING))
                showSetting = bundle.getBoolean(CameraConfiguration.Arguments.SHOW_SETTING);

            if (bundle.containsKey(CameraConfiguration.Arguments.SHOW_LOCATION))
                isUseTimeStamp = bundle.getBoolean(CameraConfiguration.Arguments.SHOW_LOCATION);

            if (bundle.containsKey(CameraConfiguration.Arguments.MOCK_LOCATION))
                isUseMockDetection = bundle.getBoolean(CameraConfiguration.Arguments.MOCK_LOCATION);

            if (bundle.containsKey(CameraConfiguration.Arguments.FACE_DETECTION))
                isUseFaceDetection = bundle.getBoolean(CameraConfiguration.Arguments.FACE_DETECTION);

            if (bundle.containsKey(CameraConfiguration.Arguments.ENABLE_CROP))
                enableImageCrop = bundle.getBoolean(CameraConfiguration.Arguments.ENABLE_CROP);

            if (bundle.containsKey(CameraConfiguration.Arguments.DEVAULT_LAT))
                currLat = bundle.getString(CameraConfiguration.Arguments.DEVAULT_LAT);

            if (bundle.containsKey(CameraConfiguration.Arguments.DEVAULT_LON))
                currLon = bundle.getString(CameraConfiguration.Arguments.DEVAULT_LON);

            if (bundle.containsKey(CameraConfiguration.Arguments.FLASH_MODE))
                switch (bundle.getInt(CameraConfiguration.Arguments.FLASH_MODE)) {
                    case CameraConfiguration.FLASH_MODE_AUTO:
                        flashMode = CameraConfiguration.FLASH_MODE_AUTO;
                        break;
                    case CameraConfiguration.FLASH_MODE_ON:
                        flashMode = CameraConfiguration.FLASH_MODE_ON;
                        break;
                    case CameraConfiguration.FLASH_MODE_OFF:
                        flashMode = CameraConfiguration.FLASH_MODE_OFF;
                        break;
                    default:
                        flashMode = CameraConfiguration.FLASH_MODE_AUTO;
                        break;
                }
            if (bundle.containsKey(CameraConfiguration.Arguments.AUTO_RECORD)) {
                if (mediaAction == CameraConfiguration.MEDIA_ACTION_VIDEO) {
                    autoRecord = bundle.getBoolean(CameraConfiguration.Arguments.AUTO_RECORD);
                }
            }
        }
    }

    @Override
    View getUserContentView(LayoutInflater layoutInflater, ViewGroup parent) {
        cameraControlPanel = (CameraControlPanel) layoutInflater.inflate(R.layout.user_control_layout, parent, false);

        if (cameraControlPanel != null) {
            cameraControlPanel.setup(getMediaAction());

            switch (flashMode) {
                case CameraConfiguration.FLASH_MODE_AUTO:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_AUTO);
                    break;
                case CameraConfiguration.FLASH_MODE_ON:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_ON);
                    break;
                case CameraConfiguration.FLASH_MODE_OFF:
                    cameraControlPanel.setFlasMode(FlashSwitchView.FLASH_OFF);
                    break;
            }

            cameraControlPanel.setRecordButtonListener(this);
            cameraControlPanel.setFlashModeSwitchListener(this);
            cameraControlPanel.setOnMediaActionStateChangeListener(this);
            cameraControlPanel.setOnCameraTypeChangeListener(this);
            cameraControlPanel.setOnSoundTypeChangeListener(this);
            cameraControlPanel.setMaxVideoDuration(getVideoDuration());
            cameraControlPanel.setMaxVideoFileSize(getVideoFileSize());
            cameraControlPanel.setSettingsClickListener(this);
            cameraControlPanel.setPickerItemClickListener(this);
            cameraControlPanel.shouldShowCrop(enableImageCrop);
            cameraControlPanel.setOnTrackerOnDrawListener(this);

            cameraControlPanel.setCameraSoundView(Utils.getSoundType(getActivity()));
            cameraControlPanel.setDevaultSoundType(Utils.getSoundType(getActivity()));

            if (autoRecord) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cameraControlPanel.startRecording();
                    }
                }, 1500);
            }
        }
        return cameraControlPanel;
    }

    @Override
    public void onSettingsClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (currentMediaActionState == MediaActionSwitchView.ACTION_VIDEO) {
            builder.setSingleChoiceItems(videoQualities, getVideoOptionCheckedIndex(), getVideoOptionSelectedListener());
            if (getVideoFileSize() > 0)
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title),
                        "(Max " + getVideoFileSize() / (1024 * 1024) + " MB)"));
            else
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title), ""));
        } else {
            builder.setSingleChoiceItems(photoQualities, getPhotoOptionCheckedIndex(), getPhotoOptionSelectedListener());
            builder.setTitle(R.string.settings_photo_quality_title);
        }

        builder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (newQuality > 0 && newQuality != mediaQuality) {
                    mediaQuality = newQuality;
                    dialogInterface.dismiss();
                    cameraControlPanel.lockControls();
                    getCameraController().switchQuality();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        settingsDialog = builder.create();
        settingsDialog.show();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(settingsDialog.getWindow().getAttributes());
        layoutParams.width = Utils.convertDpToPixel(350);
        layoutParams.height = Utils.convertDpToPixel(350);
        settingsDialog.getWindow().setAttributes(layoutParams);
    }

    @Override
    public void onItemClick(View view, int position) {
        String filePath = mediaList.get(position).getPath();
        int mimeType = getMimeType(getApplicationContext(), filePath);
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ChareemCamera.MEDIA, new Media(mimeType, filePath));
        setResult(RESULT_OK, resultIntent);
        this.finish();
    }

    @Override
    public void onSoundTypeChanged(@CameraSoundView.SoundType int soundType) {
        Utils.saveSoundType(getActivity(), soundType);
        cameraControlPanel.setCameraSoundView(soundType);
    }

    @Override
    public void onCameraTypeChanged(@CameraSwitchView.CameraType int cameraType) {
        if (currentCameraType == cameraType) return;
        currentCameraType = cameraType;

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);

        int cameraFace = cameraType == CameraSwitchView.CAMERA_TYPE_FRONT
                ? CameraConfiguration.CAMERA_FACE_FRONT : CameraConfiguration.CAMERA_FACE_REAR;

        getCameraController().switchCamera(cameraFace);
    }


    @Override
    public void onFlashModeChanged(@FlashSwitchView.FlashMode int mode) {
        switch (mode) {
            case FlashSwitchView.FLASH_AUTO:
                flashMode = CameraConfiguration.FLASH_MODE_AUTO;
                getCameraController().setFlashMode(CameraConfiguration.FLASH_MODE_AUTO);
                break;
            case FlashSwitchView.FLASH_ON:
                flashMode = CameraConfiguration.FLASH_MODE_ON;
                getCameraController().setFlashMode(CameraConfiguration.FLASH_MODE_ON);
                break;
            case FlashSwitchView.FLASH_OFF:
                flashMode = CameraConfiguration.FLASH_MODE_OFF;
                getCameraController().setFlashMode(CameraConfiguration.FLASH_MODE_OFF);
                break;
        }
    }


    @Override
    public void onMediaActionChanged(int mediaActionState) {
        if (currentMediaActionState == mediaActionState) return;
        currentMediaActionState = mediaActionState;
    }

    @Override
    public void onTakePhotoButtonPressed() {
        getCameraController().takePhoto();
    }

    @Override
    public void onStartRecordingButtonPressed() {
        getCameraController().startVideoRecord();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        getCameraController().stopVideoRecord();
    }

    @Override
    protected void onScreenRotation(int degrees) {
        cameraControlPanel.rotateControls(degrees);
        rotateSettingsDialog(degrees);
    }

    @Override
    public int getMediaAction() {
        return mediaAction;
    }

    @Override
    public int getMediaQuality() {
        return mediaQuality;
    }

    @Override
    public int getVideoDuration() {
        return videoDuration;
    }

    @Override
    public long getVideoFileSize() {
        return videoFileSize;
    }

    @Override
    public int getFlashMode() {
        return flashMode;
    }

    @Override
    public int getMinimumVideoDuration() {
        return minimumVideoDuration / 1000;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void updateCameraPreview(Size size, View cameraPreview) {
        cameraControlPanel.unLockControls();
        cameraControlPanel.allowRecord(true);

        setCameraPreview(cameraPreview, size);
    }

    @Override
    public void updateUiForMediaAction(@CameraConfiguration.MediaAction int mediaAction) {

    }

    @Override
    public void updateCameraSwitcher(int numberOfCameras) {
        cameraControlPanel.allowCameraSwitching(numberOfCameras > 1);
    }

    @Override
    public void onPhotoTaken() {
        startPreviewActivity();
    }

    @Override
    public void onVideoRecordStart(int width, int height) {
        cameraControlPanel.onStartVideoRecord(getCameraController().getOutputFile());
    }

    @Override
    public void onVideoRecordStop() {
        cameraControlPanel.allowRecord(false);
        cameraControlPanel.onStopVideoRecord();
        startPreviewActivity();
    }

    @Override
    public void releaseCameraPreview() {
        clearCameraPreview();
    }

    private void startPreviewActivity() {
        try {
            if (getMediaAction() == CameraConfiguration.MEDIA_ACTION_PHOTO){
                final Bitmap rotatedBitmap = Utils.rotateImageIfRequired(getCameraController().getOutputFile().toString(), this);
                saveBitmap(rotatedBitmap);
                Intent intent = PreviewActivity.newIntent(BaseChareemActivity.this,
                        getMediaAction(), getCameraController().getOutputFile().toString(), cameraControlPanel.showCrop());
                startActivityForResult(intent, REQUEST_PREVIEW_CODE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showToast(e.getMessage() +" ");
        }
    }

    private void saveBitmap(Bitmap rotatedBitmap){
        if (isUseTimeStamp && !addressText.equals("")){
            rotatedBitmap = Utils.resize(rotatedBitmap, 1280, 1280);
            rotatedBitmap = Utils.drawMultilineTextToBitmap(this, rotatedBitmap, addressText, 10);
        }
        Utils.saveBitmap(getCameraController().getOutputFile().toString(), rotatedBitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PREVIEW_CODE) {
                if (PreviewActivity.isResultConfirm(data)) {
                    String filePath = PreviewActivity.getMediaFilePatch(data);
                    int mimeType = getMimeType(getApplicationContext(), filePath);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(ChareemCamera.MEDIA, new Media(mimeType, filePath));
                    setResult(RESULT_OK, resultIntent);
                    this.finish();
                } else if (PreviewActivity.isResultCancel(data)) {
                    //ignore, just proceed the camera
                    this.finish();
                } else if (PreviewActivity.isResultRetake(data)) {
                    //ignore, just proceed the camera
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static int getMimeType(Context context, String path) {
        Uri uri = Uri.fromFile(new File(path));
        String extension;
        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(path);
        }
        String mimeTypeString
                = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        int mimeType = ChareemCamera.MediaType.PHOTO;
        if (mimeTypeString.toLowerCase().contains("video")) {
            mimeType = ChareemCamera.MediaType.VIDEO;
        }
        return mimeType;
    }

    private void rotateSettingsDialog(int degrees) {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            ViewGroup dialogView = (ViewGroup) settingsDialog.getWindow().getDecorView();
            for (int i = 0; i < dialogView.getChildCount(); i++) {
                dialogView.getChildAt(i).setRotation(degrees);
            }
        }
    }

    protected abstract CharSequence[] getVideoQualityOptions();

    protected abstract CharSequence[] getPhotoQualityOptions();

    protected int getVideoOptionCheckedIndex() {
        int checkedIndex = -1;
        if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_AUTO) checkedIndex = 0;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) checkedIndex = 1;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) checkedIndex = 2;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOW) checkedIndex = 3;

        if (passedMediaQuality != CameraConfiguration.MEDIA_QUALITY_AUTO) checkedIndex--;

        return checkedIndex;
    }

    protected int getPhotoOptionCheckedIndex() {
        int checkedIndex = -1;
        if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGHEST) checkedIndex = 0;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_HIGH) checkedIndex = 1;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_MEDIUM) checkedIndex = 2;
        else if (mediaQuality == CameraConfiguration.MEDIA_QUALITY_LOWEST) checkedIndex = 3;
        return checkedIndex;
    }

    protected DialogInterface.OnClickListener getVideoOptionSelectedListener() {
        return new DialogInterface.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                newQuality = ((VideoQualityOption) videoQualities[index]).getMediaQuality();
            }
        };
    }

    protected DialogInterface.OnClickListener getPhotoOptionSelectedListener() {
        return new DialogInterface.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                newQuality = ((PhotoQualityOption) photoQualities[index]).getMediaQuality();
            }
        };
    }

    private void fetchMediaList() {
        switch (mediaAction) {
            case CameraConfiguration.MEDIA_ACTION_PHOTO:
                addPhotosToList();
                break;
            case CameraConfiguration.MEDIA_ACTION_VIDEO:
                addVideosToList();
                break;
            case CameraConfiguration.MEDIA_ACTION_BOTH:
                addPhotosToList();
                addVideosToList();
                break;
        }
        cameraControlPanel.setMediaList(mediaList);
    }

    private void addPhotosToList() {
        Cursor imageCursor;
        String[] columns = {MediaStore.Images.Media.DATA};
        String orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC";
        imageCursor = getApplicationContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
        addToMediaList(imageCursor, ChareemCamera.MediaType.PHOTO);
    }

    private void addVideosToList() {
        String[] columns = {MediaStore.Video.VideoColumns.DATA};
        String orderBy = MediaStore.Video.Media.DATE_ADDED + " DESC";

        Cursor videoCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
        addToMediaList(videoCursor, ChareemCamera.MediaType.VIDEO);
    }

    private void addToMediaList(Cursor cursor, final int type) {
        try {
            while (cursor.moveToNext()) {
                String imageLocation = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Media media = new Media();
                media.setType(type);
                media.setPath(imageLocation);
                mediaList.add(media);
            }
        } finally {
            cursor.close();
        }
    }

    private void initLocation(){
        int timeout = 60000;
        gps.beginUpdates();
        gps.setListener(new SimpleLocation.Listener() {
            @Override
            public void onPositionChanged() {
                currLat = gps.getLatitude()+"";
                currLon = gps.getLongitude()+"";
                if (isUseMockDetection){
                    if (Utils.isMockLocationOn(gps.getLocation())){
                        showDialogMock(getActivity());
                        return;
                    }
                }
                setLocation();
            }
        });
        fusedLocation = new FusedLocation(this, true);
        fusedLocation.init();
        fusedLocation.setTimeout(timeout);
        fusedLocation.setListener(new FusedLocation.Listener(){
            @Override
            public void onGetLocation(Location location) {
                if (location != null) {
                    currLat = location.getLatitude()+"";
                    currLon = location.getLongitude()+"";
                    if (isUseMockDetection){
                        if (Utils.isMockLocationOn(location)){
                            showDialogMock(getActivity());
                            return;
                        }
                    }
                    setLocation();
                } else Toast.makeText(getApplication(), "Failed get location", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onTimeout() {
                Toast.makeText(getApplication(), "Time out get location", Toast.LENGTH_LONG).show();
            }

        });
        fusedLocation.getSingleUpdate();
    }

    private void setLocation(){
        if (currLat.equals("") || currLat.equals("0") || currLon.equals("") || currLon.equals("0")) {
            Toast.makeText(getApplication(), "Location not found, please check your GPS and Internet connection!!!", Toast.LENGTH_LONG).show();
            return;
        }
        Address address = Utils.getAddress(this, Double.parseDouble(currLat) , Double.parseDouble(currLon));
        if (address != null){
            String text = "";
            text += address.getThoroughfare() == null ? "" : address.getThoroughfare();
            text += "\n";
            text += address.getSubLocality() == null ? "" : address.getSubLocality();
            text += " , ";
            text += address.getLocality() == null ? "" : address.getLocality();
            text += "\n";
            text += address.getPostalCode() == null ? "" : address.getPostalCode();
            text += " , ";
            text += address.getSubAdminArea() == null ? "" : address.getSubAdminArea();
            text += "\n";
            text += address.getAdminArea() == null ? "" : address.getAdminArea();
            text += " , ";
            text += address.getCountryName() == null ? "" : address.getCountryName();
            text += "\n";
            text += "Lat : "+currLat;
            text += "\n";
            text += "Lon : "+currLon;

            String text2 = "";
            String tgl = Utils.getCurrentTimeStr("dd MMM yyyy HH:mm:ss");
            text2 += "\n";
            text2 += tgl;

            this.addressText = text;

            cameraControlPanel.tvLocation().setText(text);
            cameraControlPanel.tvLocation().append(text2);
        }
    }

    private void showDialogMock(final Activity activity){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        // Setting Dialog Title
        alertDialog.setTitle("Mock Location is ON");

        // Setting Dialog Message
        alertDialog.setMessage("Application fake GPS is on, turn off it first");
        AlertDialog dialogs = alertDialog.create();
        dialogs.setButton(
                DialogInterface.BUTTON_POSITIVE, "Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                }
        );
        /*dialogs.setButton(
            DialogInterface.BUTTON_NEGATIVE, "Cancel"
        ) { dialog: DialogInterface?, which: Int -> context.finish() }*/
        dialogs.setCancelable(false);

        dialogs.show();
    }

    @Override
    public void updateFace(Bitmap bitmap, Size previewSize, Integer sensorOrientation){

    }

    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;

        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;

    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void updateResults(final long currTimestamp, final List<SimilarityClassifier.Recognition> mappedRecognitions) {
        //adding = false;

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        computingDetection = false;
                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        cameraControlPanel.setFaceView();
                    }
                });

    }

    @Override
    public void onTracker(Canvas canvas){

    }
}
