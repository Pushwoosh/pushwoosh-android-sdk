package com.pushwoosh.internal.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.DisplayMetrics;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.fileprovider.PWFileProvider;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class NotificationUtils {
    private static final String DEFAULT_SOUND_NAME = "default";

    enum TypeContent {
        sound, image
    }

    public static int tryToGetIconFormStringOrGetFromApplication(String iconName) {
        // 4th priority: Application icon. The bad thing it is 48x48, on 1dp device notification icon should be 24x24, and it will be cropped :(
        int iconId = -1;

        // 3d priority: 'pw_notification' resource name
        int notificationId = AndroidPlatformModule.getResourceProvider().getIdentifier("pw_notification", "drawable");
        if (notificationId != 0) {
            iconId = notificationId;
        }

        // 2nd priority: AndroidManifest.xml metadata
        // PushwooshPlatform.getInstance() might return null when opening an app with existing notifications
        if (PushwooshPlatform.getInstance() != null && PushwooshPlatform.getInstance().getConfig() != null) {
            notificationId = PushwooshPlatform.getInstance().getConfig().getNotificationIcon();
            if (notificationId != 0) {
                iconId = notificationId;
            }
        }


        // 1st priority: remote notification icon id
        if (null != iconName) {
            //Try to get from the string we have received
            int customId = AndroidPlatformModule.getResourceProvider().getIdentifier(iconName, "drawable");
            if (0 != customId) {
                iconId = customId;
            }
        }

        return iconId;
    }

    private static float getNewImageScale(int imageSize, int outWidth, int outHeight) {
        int maxSize = Math.max(outWidth, outHeight);
        float newImageScale = 1f;
        if (-1 != imageSize) {
            DisplayMetrics displayMetrics = AndroidPlatformModule.getResourceProvider().getDisplayMetrics();
            if (displayMetrics != null) {
                newImageScale = maxSize / (imageSize * displayMetrics.density);
            }
        }
        return newImageScale;
    }

    @Nullable
    public static Uri getSoundUri(String sound) {
        NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        AudioManager am = AndroidPlatformModule.getManagerProvider().getAudioManager();
        if (am != null) {
            if (!(notificationPrefs.soundType().get() == SoundType.ALWAYS
                    || (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL && notificationPrefs.soundType().get() == SoundType.DEFAULT_MODE))) {
                // if always or normal type not set
                return null;
            }

            if (sound == null || sound.equals(DEFAULT_SOUND_NAME)) {
                return defaultSound;
            }

            if (sound.length() == 0) {
                return null;
            }
        }

        int soundId = AndroidPlatformModule.getResourceProvider().getIdentifier(sound, "raw");
        if (0 != soundId) {
            // if found valid resource id
            return Uri.parse("android.resource://" + AndroidPlatformModule.getAppInfoProvider().getPackageName() + "/" + soundId);
        }

        //try to open from file:///android_asset/www/res for Phonegap
        Uri uri = NotificationUtils.getUriForAssetPath("www/res/" + sound, TypeContent.sound);
        if (uri != Uri.EMPTY) {
            return uri;
        }

        return defaultSound;
    }

    public static Bitmap getScaleBitmap(Bitmap srcBitmap, int outHeightSimple) {
        DisplayMetrics displayMetrics = AndroidPlatformModule.getResourceProvider().getDisplayMetrics();
        int outHeight = displayMetrics == null ? outHeightSimple : (int) (outHeightSimple * displayMetrics.density);
        return Bitmap.createScaledBitmap(srcBitmap, srcBitmap.getWidth() * outHeight / srcBitmap.getHeight(), outHeight, true);
    }

    public static boolean phoneHaveVibratePermission() {
        PackageManager packageManager = AndroidPlatformModule.getManagerProvider().getPackageManager();
        if (packageManager == null) {
            return false;
        }
        // check permission
        try {
            int result = packageManager.checkPermission(Manifest.permission.VIBRATE, AndroidPlatformModule.getAppInfoProvider().getPackageName());
            if (result == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        } catch (Exception e) {
            PWLog.error("error in checking vibrate permission", e);
        }
        return false;
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * The URI for an asset.
     *
     * @param path The given asset path
     * @return The URI pointing to the given path
     */
    private static Uri getUriForAssetPath(String path, TypeContent typeContent) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        File dir = AndroidPlatformModule.getAppInfoProvider().getExternalCacheDir();
        if (dir == null) {
            PWLog.error("Asset", "Missing external cache dir");
            return Uri.EMPTY;
        }

        String storage = dir.toString() + "/com.pushwoosh.noitfysnd";
        File file = new File(storage, fileName);
        File storageFile = new File(storage);
        if (!storageFile.exists()) {
            if (!storageFile.mkdir()) {
                PWLog.error("Asset", "Storage file not created");
                return Uri.EMPTY;
            }
        }
        AssetManager assets = AndroidPlatformModule.getManagerProvider().getAssets();
        if (assets == null) {
            return Uri.EMPTY;
        }
        try (InputStream inputStream = assets.open(path)) {
            FileOutputStream outStream = new FileOutputStream(file);
            copyFile(inputStream, outStream);
            outStream.flush();
            outStream.close();
            if (typeContent == TypeContent.sound) {
                return getUriByContentProvider(file);
            } else {
                return Uri.fromFile(file);
            }
        } catch (Throwable t) {
            PWLog.error("Asset", "File not found: assets/" + path);
            PWLog.exception(t);
        }

        return Uri.EMPTY;
    }

    private static Uri getUriByContentProvider(File file) {
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context != null) {
            Uri uri = PWFileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            context.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return uri;
        }
        return Uri.fromFile(file);
    }

    @SuppressLint("NewApi")
    public static boolean isNotificationEnabled() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return true;
        }

        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void turnScreenOn() {
        try {
            NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
            if (notificationPrefs.lightScreenOn().get()) {
                PowerManager powerManager = AndroidPlatformModule.getManagerProvider().getPowerManager();
                if (powerManager == null) {
                    return;
                }

                PowerManager.WakeLock screenLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Push");
                screenLock.acquire(1000);
            }
        } catch (Exception e) {
            // shouln't crash. It is possible we do not have required permissions
            PWLog.exception(e);
        }
    }

    public static String getPathFileInAssets(String largeIconUrl) {
        return getUriForAssetPath(largeIconUrl, TypeContent.image).getEncodedPath();
    }

    public static Bitmap tryGetBitmap(String largeIconUrl, int dimension) {
        if(isValidURL(largeIconUrl)){
            return NotificationUtils.tryToGetBitmapFromInternet(largeIconUrl, dimension);
        }else {
            return NotificationUtils.tryToGetBitmapFromDisk(NotificationUtils.getPathFileInAssets(largeIconUrl), dimension);
        }
    }

    public static boolean isValidURL(String urlString)
    {
        try
        {
            URL url = new URL(urlString);
            url.toURI();
            return true;
        } catch (Exception exception)
        {
            return false;
        }
    }

    public static Bitmap tryToGetBitmapFromInternet(String bitmapUrl, int imageSize) {
        if (null != bitmapUrl) {
            try {
                URL url = new URL(bitmapUrl);
                URLConnection connection = url.openConnection();
                setDefaultBitmapTimeout(connection);
                connection.connect();

                byte[] buffer = new byte[1024];
                InputStream inputStream = connection.getInputStream();
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    try {
                        int read;

                        while ((read = inputStream.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, read);
                        }

                        buffer = byteArrayOutputStream.toByteArray();
                    } finally {
                        byteArrayOutputStream.close();
                    }
                } finally {
                    inputStream.close();
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);

                int outWidth = options.outWidth;
                int outHeight = options.outHeight;
                float newImageScale = getNewImageScale(imageSize, outWidth, outHeight);

                options.inJustDecodeBounds = false;
                options.inSampleSize = Math.round(newImageScale);

                return BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
            } catch (Throwable e) {
                PWLog.error("Can't load image: " + bitmapUrl, e);
            }
        }

        return null;
    }

    public static Bitmap tryToGetBitmapFromDisk(String largeIconUrl, int imageSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(largeIconUrl, options);
        options.inJustDecodeBounds = false;

        float newImageScale = getNewImageScale(imageSize, options.outWidth, options.outHeight);
        options.inSampleSize =  Math.round(newImageScale);
        return BitmapFactory.decodeFile(largeIconUrl, options);
    }

    private static void setDefaultBitmapTimeout(URLConnection connection) {
        boolean handleUsingWorkManager = RepositoryModule.getNotificationPreferences() != null &&
                RepositoryModule.getNotificationPreferences().handleNotificationsUsingWorkManager().get();
        if (!handleUsingWorkManager) {
            int twoSeconds = 2000;
            connection.setConnectTimeout(twoSeconds);
        }
    }
}
