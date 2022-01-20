package com.chareem.customCamera.sujaul.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Surface;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import androidx.core.content.res.ResourcesCompat;

import com.chareem.customCamera.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Arpit Gandhi on 7/18/16.
 */
public class Utils {

    public static int getDeviceDefaultOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Configuration config = context.getResources().getConfiguration();

        int rotation = windowManager.getDefaultDisplay().getRotation();

        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else {
            return Configuration.ORIENTATION_PORTRAIT;
        }
    }

    public static String getMimeType(String url) {
        String type = "";
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (!TextUtils.isEmpty(extension)) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        } else {
            String reCheckExtension = MimeTypeMap.getFileExtensionFromUrl(url.replaceAll("\\s+", ""));
            if (!TextUtils.isEmpty(reCheckExtension)) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(reCheckExtension);
            }
        }
        return type;
    }

    public static int convertDipToPixels(Context context, int dip) {
        Resources resources = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, resources.getDisplayMetrics());
        return (int) px;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int convertDpToPixel(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static Address getAddress(Context context, Double lat, Double lng)  {
        try {
            Locale locale = Locale.getDefault();

            Geocoder geocoder = new Geocoder(context, locale);

            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

            return addresses.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCurrentTimeStr(String formatStr)  {
        SimpleDateFormat format = new SimpleDateFormat(formatStr, Locale.US);
        Calendar cal = Calendar.getInstance();
        String tgl = format.format(cal.getTime());

        return tgl;
    }

    public static Boolean isMockLocationOn(
            Location location
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            return location.isFromMockProvider();
        else return false;
    }

    public static Bitmap drawMultilineTextToBitmap(Context gContext,
                                                   Bitmap gResId,
                                                   String gText,
                                                   Integer textSize) {
        if (textSize == null) textSize = 12;

        // prepare canvas
        Resources resources = gContext.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap = gResId;

        android.graphics.Bitmap.Config bitmapConfig = bitmap.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);

        // new antialiased Paint
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.WHITE);
        // text size in pixels
        paint.setTextSize((int) (textSize * scale));
        // text shadow
        //paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        Typeface customTypeface = ResourcesCompat.getFont(gContext, R.font.opensans_semibold);
        paint.setTypeface(customTypeface);


        // set text width to canvas width minus 16dp padding
        int textWidth = canvas.getWidth() - (int) (16 * scale);

        // init StaticLayout for text
        StaticLayout textLayout = new StaticLayout(
                gText, paint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 2.0f, false
        );

        // get height of multiline text
        int textHeight = textLayout.getHeight();

        // get position of text's top left corner
        int x = (bitmap.getWidth() - textWidth)/2;
        int y = (bitmap.getHeight() - textHeight)*98/100;

        // draw text to the Canvas center
        canvas.save();
        canvas.translate(x, y);
        textLayout.draw(canvas);
        canvas.restore();

        return bitmap;
    }

    public static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        try {
            if (image != null){
                if (maxHeight > 0 && maxWidth > 0) {
                    float width = image.getWidth();
                    float height = image.getHeight();
                    float ratioBitmap = width / height;
                    float ratioMax = (float) maxWidth / (float) maxHeight;

                    int finalWidth = maxWidth;
                    int finalHeight = maxHeight;
                    if (ratioMax > ratioBitmap) {
                        finalWidth = (int) (maxHeight * ratioBitmap);
                    } else {
                        finalHeight = (int) (maxWidth / ratioBitmap);
                    }
                    image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
                    return image;
                } else {
                    return image;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Bitmap rotateImageIfRequired(String selectedImage, Context context) throws IOException {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(selectedImage, bmOptions);

        InputStream input = context.getContentResolver().openInputStream(Uri.fromFile(new File(selectedImage)));
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage);

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(bitmap, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(bitmap, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(bitmap, 270);
            default:
                return bitmap;
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public static void saveBitmap(String path, Bitmap bitmap){
        File file = new  File(path);

        try {
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveSoundType(Context context, int soundType){
        SharedPreferences preferences = context.getSharedPreferences("chareem_camera_setting.conf", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEdt = preferences.edit();
        prefEdt.putInt("sound_on_off", soundType);
        prefEdt.apply();
    }

    public static int getSoundType(Context context){
        SharedPreferences preferences = context.getSharedPreferences("chareem_camera_setting.conf", Context.MODE_PRIVATE);
        return preferences.getInt("sound_on_off", 1);
    }
}
