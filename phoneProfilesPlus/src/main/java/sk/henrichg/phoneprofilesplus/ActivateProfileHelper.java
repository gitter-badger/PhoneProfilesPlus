package sk.henrichg.phoneprofilesplus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static android.content.Context.POWER_SERVICE;

class ActivateProfileHelper {

    static boolean lockRefresh = false;

    static boolean disableScreenTimeoutInternalChange = false;

    static final String ADAPTIVE_BRIGHTNESS_SETTING_NAME = "screen_auto_brightness_adj";

    // Setting.Global "zen_mode"
    static final int ZENMODE_ALL = 0;
    static final int ZENMODE_PRIORITY = 1;
    static final int ZENMODE_NONE = 2;
    static final int ZENMODE_ALARMS = 3;
    @SuppressWarnings("WeakerAccess")
    static final int ZENMODE_SILENT = 99;

    //static final String EXTRA_MERGED_PROFILE = "merged_profile";
    //static final String EXTRA_FOR_PROFILE_ACTIVATION = "for_profile_activation";

    private static final String PREF_RINGER_VOLUME = "ringer_volume";
    private static final String PREF_NOTIFICATION_VOLUME = "notification_volume";
    private static final String PREF_RINGER_MODE = "ringer_mode";
    private static final String PREF_ZEN_MODE = "zen_mode";
    private static final String PREF_LOCKSCREEN_DISABLED = "lockscreenDisabled";
    //private static final String PREF_SCREEN_UNLOCKED = "screen_unlocked";
    private static final String PREF_ACTIVATED_PROFILE_SCREEN_TIMEOUT = "activated_profile_screen_timeout";
    static final String PREF_MERGED_RING_NOTIFICATION_VOLUMES = "merged_ring_notification_volumes";

    private static void doExecuteForRadios(Context context, Profile profile)
    {
        PPApplication.sleep(300);

        // setup network type
        // in array.xml, networkTypeGSMValues are 100+ values
        if (profile._deviceNetworkType >= 100) {
            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_NETWORK_TYPE, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                // in array.xml, networkTypeGSMValues are 100+ values
                setPreferredNetworkType(context, profile._deviceNetworkType - 100);
                //try { Thread.sleep(200); } catch (InterruptedException e) { }
                //SystemClock.sleep(200);
                PPApplication.sleep(200);
            }
        }

        // setup mobile data
        if (profile._deviceMobileData != 0) {
            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_MOBILE_DATA, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                boolean _isMobileData = isMobileData(context);
                //PPApplication.logE("ActivateProfileHelper.doExecuteForRadios","_isMobileData="+_isMobileData);
                boolean _setMobileData = false;
                switch (profile._deviceMobileData) {
                    case 1:
                        if (!_isMobileData) {
                            _isMobileData = true;
                            _setMobileData = true;
                        }
                        break;
                    case 2:
                        if (_isMobileData) {
                            _isMobileData = false;
                            _setMobileData = true;
                        }
                        break;
                    case 3:
                        _isMobileData = !_isMobileData;
                        _setMobileData = true;
                        break;
                }
                if (_setMobileData) {
                    setMobileData(context, _isMobileData);
                    //try { Thread.sleep(200); } catch (InterruptedException e) { }
                    //SystemClock.sleep(200);
                    PPApplication.sleep(200);
                }
            }
        }

        // setup WiFi AP
        boolean canChangeWifi = true;
        if (profile._deviceWiFiAP != 0) {
            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_WIFI_AP, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.doExecuteForRadios-start");
                WifiApManager wifiApManager = null;
                try {
                    wifiApManager = new WifiApManager(context);
                } catch (Exception ignored) {
                }
                if (wifiApManager != null) {
                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.doExecuteForRadios-wifiApManager!=null");
                    boolean setWifiAPState = false;
                    boolean isWifiAPEnabled = wifiApManager.isWifiAPEnabled();
                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.doExecuteForRadios-isWifiAPEnabled=" + isWifiAPEnabled);
                    switch (profile._deviceWiFiAP) {
                        case 1:
                            if (!isWifiAPEnabled) {
                                isWifiAPEnabled = true;
                                setWifiAPState = true;
                                canChangeWifi = false;
                            }
                            break;
                        case 2:
                            if (isWifiAPEnabled) {
                                isWifiAPEnabled = false;
                                setWifiAPState = true;
                                canChangeWifi = true;
                            }
                            break;
                        case 3:
                            isWifiAPEnabled = !isWifiAPEnabled;
                            setWifiAPState = true;
                            canChangeWifi = !isWifiAPEnabled;
                            break;
                    }
                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.doExecuteForRadios-setWifiAPState=" + setWifiAPState);
                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.doExecuteForRadios-isWifiAPEnabled=" + isWifiAPEnabled);
                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.doExecuteForRadios-canChangeWifi=" + canChangeWifi);
                    if (setWifiAPState) {
                        setWifiAP(wifiApManager, isWifiAPEnabled, context);
                        //try { Thread.sleep(200); } catch (InterruptedException e) { }
                        //SystemClock.sleep(200);
                        PPApplication.sleep(200);
                    }
                }
            }
        }

        if (canChangeWifi) {
            // setup Wi-Fi
            if (profile._deviceWiFi != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_WIFI, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    boolean isWifiAPEnabled = WifiApManager.isWifiAPEnabled(context);
                    if ((!isWifiAPEnabled) || (profile._deviceWiFi == 4)) { // only when wifi AP is not enabled, change wifi
                        PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.doExecuteForRadios-isWifiAPEnabled=false");
                        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (wifiManager != null) {
                            int wifiState = wifiManager.getWifiState();
                            boolean isWifiEnabled = ((wifiState == WifiManager.WIFI_STATE_ENABLED) || (wifiState == WifiManager.WIFI_STATE_ENABLING));
                            boolean setWifiState = false;
                            switch (profile._deviceWiFi) {
                                case 1:
                                case 4:
                                    if (!isWifiEnabled) {
                                        isWifiEnabled = true;
                                        setWifiState = true;
                                    }
                                    break;
                                case 2:
                                    if (isWifiEnabled) {
                                        isWifiEnabled = false;
                                        setWifiState = true;
                                    }
                                    break;
                                case 3:
                                case 5:
                                    isWifiEnabled = !isWifiEnabled;
                                    setWifiState = true;
                                    break;
                            }
                            if (isWifiEnabled)
                                // when wifi is enabled from profile, no disable wifi after scan
                                WifiScanJob.setWifiEnabledForScan(context, false);
                            if (setWifiState) {
                                try {
                                    wifiManager.setWifiEnabled(isWifiEnabled);
                                } catch (Exception e) {
                                    Log.e("ActivateProfileHelper.doExecuteForRadios", Log.getStackTraceString(e));
                                }
                                //try { Thread.sleep(200); } catch (InterruptedException e) { }
                                //SystemClock.sleep(200);
                                PPApplication.sleep(200);
                            }
                        }
                    }
                }
            }

            // connect to SSID
            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_CONNECT_TO_SSID, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                if (!profile._deviceConnectToSSID.equals(Profile.CONNECTTOSSID_JUSTANY)) {
                    if (Permissions.checkLocation(context)) {
                        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (wifiManager != null) {
                            int wifiState = wifiManager.getWifiState();
                            if (wifiState == WifiManager.WIFI_STATE_ENABLED) {

                                // check if wifi is connected
                                ConnectivityManager connManager = null;
                                try {
                                    connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                                } catch (Exception ignored) {
                                    // java.lang.NullPointerException: missing IConnectivityManager
                                    // Dual SIM?? Bug in Android ???
                                }
                                if (connManager != null) {
                                    NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
                                    boolean wifiConnected = (activeNetwork != null) &&
                                            (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) &&
                                            activeNetwork.isConnected();
                                    WifiInfo wifiInfo = null;
                                    if (wifiConnected)
                                        wifiInfo = wifiManager.getConnectionInfo();

                                    List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                                    if (list != null) {
                                        for (WifiConfiguration i : list) {
                                            if (i.SSID != null && i.SSID.equals(profile._deviceConnectToSSID)) {
                                                if (wifiConnected) {
                                                    if (!wifiInfo.getSSID().equals(i.SSID)) {

                                                        if (PhoneProfilesService.getInstance() != null)
                                                            PhoneProfilesService.getInstance().connectToSSIDStarted = true;

                                                        // connected to another SSID
                                                        wifiManager.disconnect();
                                                        wifiManager.enableNetwork(i.networkId, true);
                                                        wifiManager.reconnect();
                                                    }
                                                } else
                                                    wifiManager.enableNetwork(i.networkId, true);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //else {
                //    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                //    int wifiState = wifiManager.getWifiState();
                //    if  (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                //        wifiManager.disconnect();
                //        wifiManager.reconnect();
                //    }
                //}
                if (PhoneProfilesService.getInstance() != null)
                    PhoneProfilesService.getInstance().connectToSSID = profile._deviceConnectToSSID;
            }
        }

        // setup bluetooth
        if (profile._deviceBluetooth != 0) {
            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_BLUETOOTH, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                PPApplication.logE("ActivateProfileHelper.doExecuteForRadios","setBluetooth");
                BluetoothAdapter bluetoothAdapter = BluetoothScanJob.getBluetoothAdapter(context);
                if (bluetoothAdapter != null) {
                    boolean isBluetoothEnabled = bluetoothAdapter.isEnabled();
                    boolean setBluetoothState = false;
                    switch (profile._deviceBluetooth) {
                        case 1:
                            if (!isBluetoothEnabled) {
                                isBluetoothEnabled = true;
                                setBluetoothState = true;
                            }
                            break;
                        case 2:
                            if (isBluetoothEnabled) {
                                isBluetoothEnabled = false;
                                setBluetoothState = true;
                            }
                            break;
                        case 3:
                            isBluetoothEnabled = !isBluetoothEnabled;
                            setBluetoothState = true;
                            break;
                    }
                    PPApplication.logE("ActivateProfileHelper.doExecuteForRadios", "setBluetoothState="+setBluetoothState);
                    PPApplication.logE("ActivateProfileHelper.doExecuteForRadios", "isBluetoothEnabled="+isBluetoothEnabled);
                    if (isBluetoothEnabled) {
                        // when bluetooth is enabled from profile, no disable bluetooth after scan
                        PPApplication.logE("ActivateProfileHelper.doExecuteForRadios", "isBluetoothEnabled=true; setBluetoothEnabledForScan=false");
                        BluetoothScanJob.setBluetoothEnabledForScan(context, false);
                    }
                    if (setBluetoothState) {
                        if (isBluetoothEnabled)
                            bluetoothAdapter.enable();
                        else
                            bluetoothAdapter.disable();
                    }
                }
            }
        }

        // setup GPS
        if (profile._deviceGPS != 0) {
            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_GPS, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                //String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

                boolean isEnabled = false;
                boolean ok = true;
                /*if (android.os.Build.VERSION.SDK_INT < 19)
                    isEnabled = Settings.Secure.isLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER);
                else {*/
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager != null)
                        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    else
                        ok = false;
                //}
                if (ok) {
                    PPApplication.logE("ActivateProfileHelper.doExecuteForRadios", "isEnabled=" + isEnabled);
                    switch (profile._deviceGPS) {
                        case 1:
                            setGPS(context, true);
                            break;
                        case 2:
                            setGPS(context, false);
                            break;
                        case 3:
                            if (!isEnabled) {
                                setGPS(context, true);
                            } else {
                                setGPS(context, false);
                            }
                            break;
                    }
                }
            }
        }

        // setup NFC
        if (profile._deviceNFC != 0) {
            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_NFC, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
                if (nfcAdapter != null) {
                    switch (profile._deviceNFC) {
                        case 1:
                            setNFC(context, true);
                            break;
                        case 2:
                            setNFC(context, false);
                            break;
                        case 3:
                            if (!nfcAdapter.isEnabled()) {
                                setNFC(context, true);
                            } else if (nfcAdapter.isEnabled()) {
                                setNFC(context, false);
                            }
                            break;
                    }
                }
            }
        }

    }

    private static void executeForRadios(final Profile profile, final Context context)
    {
        final Context appContext = context.getApplicationContext();
        PPApplication.startHandlerThreadRadios();
        final Handler handler = new Handler(PPApplication.handlerThreadRadios.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

                PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivateProfileHelper.executeForRadios");
                    wakeLock.acquire(10 * 60 * 1000);
                }

                boolean _isAirplaneMode = false;
                boolean _setAirplaneMode = false;
                if (profile._deviceAirplaneMode != 0) {
                    if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_AIRPLANE_MODE, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                        _isAirplaneMode = isAirplaneMode(context);
                        switch (profile._deviceAirplaneMode) {
                            case 1:
                                if (!_isAirplaneMode) {
                                    _isAirplaneMode = true;
                                    _setAirplaneMode = true;
                                }
                                break;
                            case 2:
                                if (_isAirplaneMode) {
                                    _isAirplaneMode = false;
                                    _setAirplaneMode = true;
                                }
                                break;
                            case 3:
                                _isAirplaneMode = !_isAirplaneMode;
                                _setAirplaneMode = true;
                                break;
                        }
                    }
                }

                if (_setAirplaneMode /*&& _isAirplaneMode*/) {
                    // switch ON airplane mode, set it before doExecuteForRadios
                    setAirplaneMode(/*context, */_isAirplaneMode);
                    PPApplication.sleep(2500);
                    PPApplication.logE("ActivateProfileHelper.executeForRadios", "after sleep");
                }

                doExecuteForRadios(context, profile);

                /*if (_setAirplaneMode && (!_isAirplaneMode)) {
                    // 200 milliseconds is in doExecuteForRadios
                    PPApplication.sleep(1800);

                    // switch OFF airplane mode, set if after executeForRadios
                    setAirplaneMode(context, _isAirplaneMode);
                }*/

                //PPApplication.sleep(500);

                if ((wakeLock != null) && wakeLock.isHeld()) {
                    try {
                        wakeLock.release();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    static boolean isAudibleRinging(int ringerMode, int zenMode/*, boolean onlyVibrateSilent*/) {
        /*if (onlyVibrateSilent)
            return (!((ringerMode == Profile.RINGERMODE_VIBRATE) || (ringerMode == Profile.RINGERMODE_SILENT) ||
                    ((ringerMode == Profile.RINGERMODE_ZENMODE) &&
                            ((zenMode == Profile.ZENMODE_ALL_AND_VIBRATE) || (zenMode == Profile.ZENMODE_PRIORITY_AND_VIBRATE)))
            ));
        else*/
            return (!((ringerMode == Profile.RINGERMODE_VIBRATE) || (ringerMode == Profile.RINGERMODE_SILENT) ||
                      ((ringerMode == Profile.RINGERMODE_ZENMODE) &&
                              ((zenMode == Profile.ZENMODE_NONE) || (zenMode == Profile.ZENMODE_ALL_AND_VIBRATE) ||
                               (zenMode == Profile.ZENMODE_PRIORITY_AND_VIBRATE) || (zenMode == Profile.ZENMODE_ALARMS)))
                     ));
    }

    private static boolean isVibrateRingerMode(int ringerMode/*, int zenMode*/) {
        return (ringerMode == Profile.RINGERMODE_VIBRATE);
    }


    private static boolean isAudibleSystemRingerMode(AudioManager audioManager, Context context) {
        /*int ringerMode = audioManager.getRingerMode();
        PPApplication.logE("ActivateProfileHelper.isAudibleSystemRingerMode", "ringerMode="+ringerMode);
        if (ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                int zenMode = getSystemZenMode(context, -1);
                return (zenMode == ActivateProfileHelper.ZENMODE_PRIORITY);
            }
            else
                return false;
        }
        else
            return true;*/
        return (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) ||
               (getSystemZenMode(context, -1) == ActivateProfileHelper.ZENMODE_PRIORITY);
    }

    /*
    private static void correctVolume0(AudioManager audioManager) {
        int ringerMode, zenMode;
        ringerMode = PPApplication.getRingerMode(context);
        zenMode = PPApplication.getZenMode(context);
        if ((ringerMode == 1) || (ringerMode == 2) || (ringerMode == 4) ||
            ((ringerMode == 5) && ((zenMode == 1) || (zenMode == 2)))) {
            // any "nonVIBRATE" ringer mode is selected
            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                // actual system ringer mode = vibrate
                // volume changed it to vibrate
                //RingerModeChangeReceiver.internalChange = true;
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 1, 0);
                PhoneProfilesService.ringingVolume = 1;
                //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_RING, 1);
            }
        }
    }
    */

    static boolean getMergedRingNotificationVolumes(Context context) {
        ApplicationPreferences.getSharedPreferences(context);
        PPApplication.logE("ActivateProfileHelper.getMergedRingNotificationVolumes", "force set="+ApplicationPreferences.applicationForceSetMergeRingNotificationVolumes(context));
        PPApplication.logE("ActivateProfileHelper.getMergedRingNotificationVolumes", "merged="+ApplicationPreferences.preferences.getBoolean(PREF_MERGED_RING_NOTIFICATION_VOLUMES, true));
        if (ApplicationPreferences.applicationForceSetMergeRingNotificationVolumes(context) > 0)
            return ApplicationPreferences.applicationForceSetMergeRingNotificationVolumes(context) == 1;
        else
            return ApplicationPreferences.preferences.getBoolean(PREF_MERGED_RING_NOTIFICATION_VOLUMES, true);
    }

    // test if ring and notification volumes are merged
    static void setMergedRingNotificationVolumes(Context context, boolean force) {
        ApplicationPreferences.getSharedPreferences(context);

        PPApplication.logE("ActivateProfileHelper.setMergedRingNotificationVolumes", "xxx");

        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        setMergedRingNotificationVolumes(context, force, editor);
        editor.apply();
    }

    static void setMergedRingNotificationVolumes(Context context, boolean force, SharedPreferences.Editor editor) {
        ApplicationPreferences.getSharedPreferences(context);

        PPApplication.logE("ActivateProfileHelper.setMergedRingNotificationVolumes", "xxx");

        if (!ApplicationPreferences.preferences.contains(PREF_MERGED_RING_NOTIFICATION_VOLUMES) || force) {
            try {
                boolean merged;
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    int ringerMode = audioManager.getRingerMode();
                    int maximumNotificationValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                    int oldRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                    int oldNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    if (oldRingVolume == oldNotificationVolume) {
                        int newNotificationVolume;
                        if (oldNotificationVolume == maximumNotificationValue)
                            newNotificationVolume = oldNotificationVolume - 1;
                        else
                            newNotificationVolume = oldNotificationVolume + 1;
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, newNotificationVolume, 0);
                        PPApplication.sleep(1000);
                        merged = audioManager.getStreamVolume(AudioManager.STREAM_RING) == newNotificationVolume;
                    } else
                        merged = false;
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, oldNotificationVolume, 0);
                    audioManager.setRingerMode(ringerMode);

                    PPApplication.logE("ActivateProfileHelper.setMergedRingNotificationVolumes", "merged=" + merged);

                    editor.putBoolean(PREF_MERGED_RING_NOTIFICATION_VOLUMES, merged);
                }
            } catch (Exception ignored) {}
        }
    }

    @SuppressLint("NewApi")
    private static void setVolumes(Context context, Profile profile, AudioManager audioManager, int linkUnlink, boolean forProfileActivation)
    {
        if (profile.getVolumeRingtoneChange()) {
            if (forProfileActivation)
                setRingerVolume(context, profile.getVolumeRingtoneValue());
        }
        if (profile.getVolumeNotificationChange()) {
            if (forProfileActivation)
                setNotificationVolume(context, profile.getVolumeNotificationValue());
        }

        int ringerMode = getRingerMode(context);
        int zenMode = getZenMode(context);

        PPApplication.logE("ActivateProfileHelper.setVolumes", "ringerMode=" + ringerMode);
        PPApplication.logE("ActivateProfileHelper.setVolumes", "zenMode=" + zenMode);
        PPApplication.logE("ActivateProfileHelper.setVolumes", "linkUnlink=" + linkUnlink);
        PPApplication.logE("ActivateProfileHelper.setVolumes", "forProfileActivation=" + forProfileActivation);

        //if (isAudibleRinging(ringerMode, zenMode, true) || (ringerMode == 0)) {
        if (isAudibleSystemRingerMode(audioManager, context) || (ringerMode == 0)) {
            // test only system ringer mode

            PPApplication.logE("ActivateProfileHelper.setVolumes", "ringer/notification/system change");

            //if (Permissions.checkAccessNotificationPolicy(context)) {

                if (forProfileActivation) {
                    if (profile.getVolumeSystemChange()) {
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, profile.getVolumeSystemValue(), 0);
                            //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_SYSTEM, profile.getVolumeSystemValue());
                            //correctVolume0(audioManager);
                        } catch (Exception ignored) { }
                    }
                }

                boolean volumesSet = false;
                TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if ((telephony != null) && getMergedRingNotificationVolumes(context) && ApplicationPreferences.applicationUnlinkRingerNotificationVolumes(context)) {
                    int callState = telephony.getCallState();
                    //if (doUnlink) {
                    //if (linkUnlink == PhoneCallBroadcastReceiver.LINKMODE_UNLINK) {
                    if (callState == TelephonyManager.CALL_STATE_RINGING) {
                        // for separating ringing and notification
                        // in ringing state ringer volumes must by set
                        // and notification volumes must not by set
                        int volume = getRingerVolume(context);
                        PPApplication.logE("ActivateProfileHelper.setVolumes", "doUnlink-RINGING  ringer volume=" + volume);
                        if (volume != -999) {
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
                                if (PhoneProfilesService.getInstance() != null)
                                    PhoneProfilesService.getInstance().ringingVolume = volume;
                                //PhoneProfilesService.notificationVolume = volume;
                                //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_RING, profile.getVolumeRingtoneValue());
                                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, 0);
                                //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_NOTIFICATION, profile.getVolumeNotificationValue());
                                //correctVolume0(audioManager);
                            } catch (Exception ignored) { }
                        }
                        volumesSet = true;
                    } else if (linkUnlink == PhoneCallBroadcastReceiver.LINKMODE_LINK) {
                        // for separating ringing and notification
                        // in not ringing state ringer and notification volume must by change
                        int volume = getRingerVolume(context);
                        PPApplication.logE("ActivateProfileHelper.setVolumes", "doUnlink-NOT RINGING-link  ringer volume=" + volume);
                        if (volume != -999) {
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
                                if (PhoneProfilesService.getInstance() != null)
                                    PhoneProfilesService.getInstance().ringingVolume = volume;
                                //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_RING, profile.getVolumeRingtoneValue());
                            } catch (Exception ignored) { }
                        }
                        volume = getNotificationVolume(context);
                        PPApplication.logE("ActivateProfileHelper.setVolumes", "doUnlink-NOT RINGING-link  notification volume=" + volume);
                        if (volume != -999) {
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, 0);
                                //PhoneProfilesService.notificationVolume = volume;
                                //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_NOTIFICATION, profile.getVolumeNotificationValue());
                            } catch (Exception ignored) { }
                        }
                        //correctVolume0(audioManager);
                        volumesSet = true;
                    } else {
                        int volume = getRingerVolume(context);
                        PPApplication.logE("ActivateProfileHelper.setVolumes", "doUnlink-NOT RINGING  ringer volume=" + volume);
                        if (volume != -999) {
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
                                if (PhoneProfilesService.getInstance() != null)
                                    PhoneProfilesService.getInstance().ringingVolume = volume;
                                //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_RING, volume);
                                //correctVolume0(audioManager);
                            } catch (Exception ignored) { }
                        }
                        volume = getNotificationVolume(context);
                        PPApplication.logE("ActivateProfileHelper.setVolumes", "doUnlink-NOT RINGING  notification volume=" + volume);
                        if (volume != -999) {
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, 0);
                                //PhoneProfilesService.notificationVolume = volume;
                                //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_NOTIFICATION, volume);
                                //correctVolume0(audioManager);
                            } catch (Exception ignored) { }
                        }
                        volumesSet = true;
                    }
                    /*}
                    else {
                        if (callState == TelephonyManager.CALL_STATE_RINGING) {
                            int volume = PPApplication.getRingerVolume(context);
                            if (volume == -999)
                                volume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                            PhoneProfilesService.ringingVolume = volume;
                        }
                    }*/
                }
                if (!volumesSet) {
                    // reverted order for disabled unlink
                    int volume;
                    if (!getMergedRingNotificationVolumes(context)) {
                        volume = getNotificationVolume(context);
                        PPApplication.logE("ActivateProfileHelper.setVolumes", "no doUnlink  notification volume=" + volume);
                        if (volume != -999) {
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, 0);
                                //PhoneProfilesService.notificationVolume = volume;
                                //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_NOTIFICATION, volume);
                                //correctVolume0(audioManager);
                                PPApplication.logE("ActivateProfileHelper.setVolumes", "notification volume set");
                            } catch (Exception ignored) { }
                        }
                    }
                    volume = getRingerVolume(context);
                    PPApplication.logE("ActivateProfileHelper.setVolumes", "no doUnlink  ringer volume=" + volume);
                    if (volume != -999) {
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
                            if (PhoneProfilesService.getInstance() != null)
                                PhoneProfilesService.getInstance().ringingVolume = volume;
                            //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_RING, volume);
                            //correctVolume0(audioManager);
                            PPApplication.logE("ActivateProfileHelper.setVolumes", "ringer volume set");
                        } catch (Exception ignored) { }
                    }
                }
            //}
            //else
            //    PPApplication.logE("ActivateProfileHelper.setVolumes", "not granted");
        }

        if (forProfileActivation) {
            if (profile.getVolumeMediaChange()) {
                // Fatal Exception: java.lang.SecurityException: Only SystemUI can disable the safe media volume:
                // Neither user 10118 nor current process has android.permission.STATUS_BAR_SERVICE.
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, profile.getVolumeMediaValue(), 0);
                    //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_MUSIC, profile.getVolumeMediaValue());
                } catch (SecurityException e) {
                    // adb shell pm grant sk.henrichg.phoneprofilesplus android.permission.WRITE_SECURE_SETTINGS
                    if (Permissions.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)) {
                        try {
                            Settings.Global.putInt(context.getContentResolver(), "audio_safe_volume_state", 2);
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, profile.getVolumeMediaValue(), 0);
                        }
                        catch (Exception ignored) {}
                    }
                    else {
                        if (PPApplication.isRooted()) {
                            synchronized (PPApplication.rootMutex) {
                                String command1 = "settings put global audio_safe_volume_state 2";
                                Command command = new Command(0, false, command1);
                                try {
                                    RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                    PPApplication.commandWait(command);
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, profile.getVolumeMediaValue(), 0);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (profile.getVolumeAlarmChange()) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, profile.getVolumeAlarmValue(), 0);
                    //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_ALARM, profile.getVolumeAlarmValue());
                } catch (Exception ignored) {}
            }
            if (profile.getVolumeVoiceChange()) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, profile.getVolumeVoiceValue(), 0);
                    //Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_VOICE, profile.getVolumeVoiceValue());
                } catch (Exception ignored) {}
            }
        }

    }

    private static void setZenMode(Context context, int zenMode, AudioManager audioManager, int ringerMode)
    {
        if (android.os.Build.VERSION.SDK_INT >= 21)
        {
            PPApplication.logE("ActivateProfileHelper.setZenMode", "zenMode=" + zenMode);
            PPApplication.logE("ActivateProfileHelper.setZenMode", "ringerMode=" + ringerMode);

            int _zenMode = getSystemZenMode(context, -1);
            PPApplication.logE("ActivateProfileHelper.setZenMode", "_zenMode (system)=" + _zenMode);
            int _ringerMode = audioManager.getRingerMode();
            PPApplication.logE("ActivateProfileHelper.setZenMode", "_ringerMode (system)=" + _ringerMode);

            if ((zenMode != ZENMODE_SILENT) && canChangeZenMode(context, false)) {
                PPApplication.logE("ActivateProfileHelper.setZenMode", "not ZENMODE_SILENT and can change zen mode");
                audioManager.setRingerMode(ringerMode);
                //try { Thread.sleep(500); } catch (InterruptedException e) { }
                //SystemClock.sleep(500);
                PPApplication.sleep(500);

                if ((zenMode != _zenMode) || (zenMode == ZENMODE_PRIORITY)) {
                    PPNotificationListenerService.requestInterruptionFilter(context, zenMode);
                    InterruptionFilterChangedBroadcastReceiver.requestInterruptionFilter(context, zenMode);
                }
            } else {
                PPApplication.logE("ActivateProfileHelper.setZenMode", "ZENMODE_SILENT or not can change zen mode");
                try {
                    switch (zenMode) {
                        case ZENMODE_SILENT:
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            //try { Thread.sleep(1000); } catch (InterruptedException e) { }
                            //SystemClock.sleep(1000);
                            PPApplication.sleep(1000);
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            break;
                        default:
                            audioManager.setRingerMode(ringerMode);
                    }
                } catch (Exception ignored) {
                    // may be produced this exception:
                    //
                    // java.lang.SecurityException: Not allowed to change Do Not Disturb state
                    //
                    // when changed is ringer mode in activated Do not disturb and
                    // GlobalGUIRoutines.activityActionExists(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, context) returns false.
                }
            }
        }
        else
            audioManager.setRingerMode(ringerMode);
    }

    private static void setVibrateWhenRinging(Context context, Profile profile, int value) {
        PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "profile="+profile);
        PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "value="+value);
        int lValue = value;
        if (profile != null) {
            switch (profile._vibrateWhenRinging) {
                case 1:
                    lValue = 1;
                    break;
                case 2:
                    lValue = 0;
                    break;
            }
        }

        PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "lValue="+lValue);
        if (lValue != -1) {
            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VIBRATE_WHEN_RINGING, context).allowed
                    == PreferenceAllowed.PREFERENCE_ALLOWED) {
                if (Permissions.checkVibrateWhenRinging(context)) {
                    if (android.os.Build.VERSION.SDK_INT < 23) {    // Not working in Android M (exception)
                        Settings.System.putInt(context.getContentResolver(), "vibrate_when_ringing", lValue);
                        PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "vibrate when ringing set (API < 23)");
                    }
                    else {
                        try {
                            Settings.System.putInt(context.getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, lValue);
                            PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "vibrate when ringing set (API >= 23)");
                        } catch (Exception ee) {
                            Log.e("ActivateProfileHelper.setVibrateWhenRinging", Log.getStackTraceString(ee));

                            if (PPApplication.isRooted() && PPApplication.settingsBinaryExists()) {
                                synchronized (PPApplication.rootMutex) {
                                    String command1 = "settings put system " + Settings.System.VIBRATE_WHEN_RINGING + " " + lValue;
                                    //if (PPApplication.isSELinuxEnforcing())
                                    //	command1 = PPApplication.getSELinuxEnforceCommand(command1, Shell.ShellContext.SYSTEM_APP);
                                    Command command = new Command(0, false, command1); //, command2);
                                    try {
                                        RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                        PPApplication.commandWait(command);
                                        PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "vibrate when ringing set (API >= 23 with root)");
                                    } catch (Exception e) {
                                        Log.e("ActivateProfileHelper.setVibrateWhenRinging", Log.getStackTraceString(e));
                                    }
                                }
                            }
                            else
                                PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "not rooted");
                        }
                    }
                }
                else
                    PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "not permission granted");
            }
            else
                PPApplication.logE("ActivateProfileHelper.setVibrateWhenRinging", "not profile preferences allowed");
        }
    }

    private static void setTones(Context context, Profile profile) {
        if (Permissions.checkProfileRingtones(context, profile, null)) {
            if (profile._soundRingtoneChange == 1) {
                if (!profile._soundRingtone.isEmpty()) {
                    try {
                        //Settings.System.putString(context.getContentResolver(), Settings.System.RINGTONE, profile._soundRingtone);
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, Uri.parse(profile._soundRingtone));
                    }
                    catch (Exception ignored){ }
                } else {
                    // selected is None tone
                    try {
                        //Settings.System.putString(context.getContentResolver(), Settings.System.RINGTONE, null);
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, null);
                    }
                    catch (Exception ignored){ }
                }
            }
            if (profile._soundNotificationChange == 1) {
                if (!profile._soundNotification.isEmpty()) {
                    try {
                        //Settings.System.putString(context.getContentResolver(), Settings.System.NOTIFICATION_SOUND, profile._soundNotification);
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION, Uri.parse(profile._soundNotification));
                    }
                    catch (Exception ignored){ }
                } else {
                    // selected is None tone
                    try {
                        //Settings.System.putString(context.getContentResolver(), Settings.System.NOTIFICATION_SOUND, null);
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION, null);
                    }
                    catch (Exception ignored){ }
                }
            }
            if (profile._soundAlarmChange == 1) {
                if (!profile._soundAlarm.isEmpty()) {
                    try {
                        //Settings.System.putString(context.getContentResolver(), Settings.System.ALARM_ALERT, profile._soundAlarm);
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM, Uri.parse(profile._soundAlarm));
                    }
                    catch (Exception ignored){ }
                } else {
                    // selected is None tone
                    try {
                        //Settings.System.putString(context.getContentResolver(), Settings.System.ALARM_ALERT, null);
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM, null);
                    }
                    catch (Exception ignored){ }
                }
            }
        }
    }

    static void executeForVolumes(final Profile profile, final boolean forProfileActivation, final Context context) {
        final Context appContext = context.getApplicationContext();
        PPApplication.startHandlerThreadVolumes();
        final Handler handler = new Handler(PPApplication.handlerThreadVolumes.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

                PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivateProfileHelper.executeForVolumes");
                    wakeLock.acquire(10 * 60 * 1000);
                }

                // link, unlink volumes during activation of profile
                // required for phone call events
                ApplicationPreferences.getSharedPreferences(context);
                int callEventType = ApplicationPreferences.preferences.getInt(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_EVENT_TYPE, PhoneCallBroadcastReceiver.CALL_EVENT_UNDEFINED);
                int linkUnlink = PhoneCallBroadcastReceiver.LINKMODE_NONE;
                if (ActivateProfileHelper.getMergedRingNotificationVolumes(context) && ApplicationPreferences.applicationUnlinkRingerNotificationVolumes(context)) {
                    if ((callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_RINGING) ||
                        (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_ENDED) ||
                        (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_MISSED_CALL)) {
                        linkUnlink = PhoneCallBroadcastReceiver.LINKMODE_UNLINK;
                        if ((callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_ENDED) ||
                            (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_MISSED_CALL))
                            linkUnlink = PhoneCallBroadcastReceiver.LINKMODE_LINK;
                    }
                }

                if (linkUnlink != PhoneCallBroadcastReceiver.LINKMODE_NONE)
                    // link, unlink is executed, not needed do it from EventsHandler
                    PhoneCallBroadcastReceiver.linkUnlinkExecuted = true;

                if (profile != null)
                    PPApplication.logE("ActivateProfileHelper.executeForVolumes", "profile.name="+profile._name);
                else
                    PPApplication.logE("ActivateProfileHelper.executeForVolumes", "profile=null");

                if ((callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_ANSWERED) ||
                        (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_OUTGOING_CALL_ANSWERED)) {
                    PhoneCallBroadcastReceiver.setSpeakerphoneOn(profile, context);
                    PhoneCallBroadcastReceiver.speakerphoneOnExecuted = true;
                }

                if (profile != null)
                {
                    setTones(context, profile);

                    if (/*Permissions.checkProfileVolumePreferences(context, profile) &&*/
                            Permissions.checkProfileAccessNotificationPolicy(context, profile, null)) {

                        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                        changeRingerModeForVolumeEqual0(profile, audioManager);
                        changeNotificationVolumeForVolumeEqual0(context, profile);

                        RingerModeChangeReceiver.internalChange = true;

                        setRingerMode(context, profile, audioManager, true, /*linkUnlink,*/ forProfileActivation);
                        PPApplication.logE("ActivateProfileHelper.executeForVolumes", "internalChange="+RingerModeChangeReceiver.internalChange);
                        // !!! DO NOT CALL setVolumes before setRingerMode(..., firsCall:false).
                        //     Ringer mode must be changed before call of setVolumes() because is checked in setVolumes().
                        setRingerMode(context, profile, audioManager, false, /*linkUnlink,*/ forProfileActivation);
                        PPApplication.logE("ActivateProfileHelper.executeForVolumes", "internalChange="+RingerModeChangeReceiver.internalChange);
                        PPApplication.sleep(500);
                        setVolumes(context, profile, audioManager, linkUnlink, forProfileActivation);
                        PPApplication.logE("ActivateProfileHelper.executeForVolumes", "internalChange="+RingerModeChangeReceiver.internalChange);

                        //try { Thread.sleep(500); } catch (InterruptedException e) { }
                        //SystemClock.sleep(500);
                        PPApplication.sleep(500);

                        /*
                        PPApplication.startHandlerThread("ActivateProfileHelper.executeForVolumes");
                        final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                PPApplication.logE("ActivateProfileHelper.executeForVolumes", "disable ringer mode change internal change");
                                RingerModeChangeReceiver.internalChange = false;
                            }
                        }, 3000);
                        */
                        PostDelayedBroadcastReceiver.setAlarm(
                                PostDelayedBroadcastReceiver.ACTION_RINGER_MODE_INTERNAL_CHANGE_TO_FALSE, 3);
                    }

                    setTones(context, profile);
                }

                if ((wakeLock != null) && wakeLock.isHeld()) {
                    try {
                        wakeLock.release();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private static void setNotificationLed(Context context, int value) {
        if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_NOTIFICATION_LED, context).allowed
                == PreferenceAllowed.PREFERENCE_ALLOWED) {
            if (android.os.Build.VERSION.SDK_INT < 23)    // Not working in Android M (exception)
                Settings.System.putInt(context.getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE, value);
            else {
                /* not working (private secure settings) :-/
                if (Permissions.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)) {
                    Settings.System.putInt(context.getContentResolver(), "notification_light_pulse", value);
                }
                else*/
                if (PPApplication.isRooted() && PPApplication.settingsBinaryExists()) {
                    synchronized (PPApplication.rootMutex) {
                        String command1 = "settings put system " + Settings.System.NOTIFICATION_LIGHT_PULSE + " " + value;
                        //if (PPApplication.isSELinuxEnforcing())
                        //	command1 = PPApplication.getSELinuxEnforceCommand(command1, Shell.ShellContext.SYSTEM_APP);
                        Command command = new Command(0, false, command1); //, command2);
                        try {
                            RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                            PPApplication.commandWait(command);
                        } catch (Exception e) {
                            Log.e("ActivateProfileHelper.setNotificationLed", Log.getStackTraceString(e));
                        }
                    }
                }
            }
        }
    }

    private static void setHeadsUpNotifications(Context context, final int value) {
        if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_HEADS_UP_NOTIFICATIONS, context).allowed
                == PreferenceAllowed.PREFERENCE_ALLOWED) {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                if (Permissions.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)) {
                    Settings.Global.putInt(context.getContentResolver(), "heads_up_notifications_enabled", value);
                }
                else
                if (PPApplication.isRooted() && PPApplication.settingsBinaryExists()) {
                    final Context appContext = context.getApplicationContext();
                    PPApplication.startHandlerThreadHeadsUpNotifications();
                    final Handler handler = new Handler(PPApplication.handlerThreadHeadsUpNotifications.getLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                            PowerManager.WakeLock wakeLock = null;
                            if (powerManager != null) {
                                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivateProfileHelper.setHeadsUpNotifications");
                                wakeLock.acquire(10 * 60 * 1000);
                            }

                            synchronized (PPApplication.rootMutex) {
                                String command1 = "settings put global " + "heads_up_notifications_enabled" + " " + value;
                                //if (PPApplication.isSELinuxEnforcing())
                                //	command1 = PPApplication.getSELinuxEnforceCommand(command1, Shell.ShellContext.SYSTEM_APP);
                                Command command = new Command(0, false, command1); //, command2);
                                try {
                                    RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                    PPApplication.commandWait(command);
                                } catch (Exception e) {
                                    Log.e("ActivateProfileHelper.setHeadsUpNotifications", Log.getStackTraceString(e));
                                }
                            }

                            if ((wakeLock != null) && wakeLock.isHeld()) {
                                try {
                                    wakeLock.release();
                                } catch (Exception ignored) {}
                            }
                        }
                    });
                }
            }
        }
    }

    private static void changeRingerModeForVolumeEqual0(Profile profile, AudioManager audioManager) {
        PPApplication.logE("ActivateProfileHelper.changeRingerModeForVolumeEqual0", "volumeRingtoneChange=" + profile.getVolumeRingtoneChange());
        PPApplication.logE("ActivateProfileHelper.changeRingerModeForVolumeEqual0", "volumeRingtoneValue=" + profile.getVolumeRingtoneValue());

        profile._ringerModeForZenMode = AudioManager.RINGER_MODE_NORMAL;

        if (profile.getVolumeRingtoneChange()) {

            if (profile.getVolumeRingtoneValue() == 0) {
                profile.setVolumeRingtoneValue(1);
                profile._ringerModeForZenMode = AudioManager.RINGER_MODE_SILENT;

                // for profile ringer/zen mode = "only vibrate" do not change ringer mode to Silent
                if (!isVibrateRingerMode(profile._volumeRingerMode/*, profile._volumeZenMode*/)) {
                    if (isAudibleRinging(profile._volumeRingerMode, profile._volumeZenMode/*, false*/)) {
                        // change ringer mode to Silent
                        PPApplication.logE("ActivateProfileHelper.changeRingerModeForVolumeEqual0", "changed to silent");
                        profile._volumeRingerMode = Profile.RINGERMODE_SILENT;
                    }
                }
            }
            else {
                if ((profile._volumeRingerMode == 0) && (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT)) {
                    // change ringer mode to Ringing
                    PPApplication.logE("ActivateProfileHelper.changeRingerModeForVolumeEqual0", "changed to ringing");
                    profile._volumeRingerMode = Profile.RINGERMODE_RING;
                }
            }
        }
    }

    private static void changeNotificationVolumeForVolumeEqual0(Context context, Profile profile) {
        PPApplication.logE("ActivateProfileHelper.changeNotificationVolumeForVolumeEqual0", "volumeNotificationChange="+profile.getVolumeNotificationChange());
        PPApplication.logE("ActivateProfileHelper.changeNotificationVolumeForVolumeEqual0", "mergedRingNotificationVolumes="+getMergedRingNotificationVolumes(context));
        PPApplication.logE("ActivateProfileHelper.changeNotificationVolumeForVolumeEqual0", "volumeNotificationValue="+profile.getVolumeNotificationValue());
        if (profile.getVolumeNotificationChange() && getMergedRingNotificationVolumes(context)) {
            if (profile.getVolumeNotificationValue() == 0) {
                PPApplication.logE("ActivateProfileHelper.changeNotificationVolumeForVolumeEqual0", "changed notification value to 1");
                profile.setVolumeNotificationValue(1);
            }
        }
    }

    static boolean canChangeZenMode(Context context, boolean notCheckAccess) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            boolean no60 = !Build.VERSION.RELEASE.equals("6.0");
            if (no60 && GlobalGUIRoutines.activityActionExists(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, context)) {
                if (notCheckAccess)
                    return true;
                else
                    return Permissions.checkAccessNotificationPolicy(context);
            }
            else
                return PPNotificationListenerService.isNotificationListenerServiceEnabled(context);
        }
        else
        if (android.os.Build.VERSION.SDK_INT >= 21)
            return PPNotificationListenerService.isNotificationListenerServiceEnabled(context);
        return false;
    }

    @SuppressLint("SwitchIntDef")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    static int getSystemZenMode(Context context, int defaultValue) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            boolean no60 = !Build.VERSION.RELEASE.equals("6.0");
            if (no60 && GlobalGUIRoutines.activityActionExists(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, context)) {
                NotificationManager mNotificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if (mNotificationManager != null) {
                    int interruptionFilter = mNotificationManager.getCurrentInterruptionFilter();
                    switch (interruptionFilter) {
                        case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                            return ActivateProfileHelper.ZENMODE_ALARMS;
                        case NotificationManager.INTERRUPTION_FILTER_ALL:
                            return ActivateProfileHelper.ZENMODE_ALL;
                        case NotificationManager.INTERRUPTION_FILTER_NONE:
                            return ActivateProfileHelper.ZENMODE_NONE;
                        case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                            return ActivateProfileHelper.ZENMODE_PRIORITY;
                        case NotificationManager.INTERRUPTION_FILTER_UNKNOWN:
                            return ActivateProfileHelper.ZENMODE_ALL;
                    }
                }
            }
            else {
                int interruptionFilter = Settings.Global.getInt(context.getContentResolver(), "zen_mode", -1);
                switch (interruptionFilter) {
                    case 0:
                        return ActivateProfileHelper.ZENMODE_ALL;
                    case 1:
                        return ActivateProfileHelper.ZENMODE_PRIORITY;
                    case 2:
                        return ActivateProfileHelper.ZENMODE_NONE;
                    case 3:
                        return ActivateProfileHelper.ZENMODE_ALARMS;
                }
            }
        }
        if ((android.os.Build.VERSION.SDK_INT >= 21) && (android.os.Build.VERSION.SDK_INT < 23)) {
            int interruptionFilter = Settings.Global.getInt(context.getContentResolver(), "zen_mode", -1);
            switch (interruptionFilter) {
                case 0:
                    return ActivateProfileHelper.ZENMODE_ALL;
                case 1:
                    return ActivateProfileHelper.ZENMODE_PRIORITY;
                case 2:
                    return ActivateProfileHelper.ZENMODE_NONE;
                case 3:
                    return ActivateProfileHelper.ZENMODE_ALARMS;
            }
        }
        return defaultValue;
    }

    static boolean vibrationIsOn(/*Context context, */AudioManager audioManager, boolean testRingerMode) {
        int ringerMode = -999;
        if (testRingerMode)
            ringerMode = audioManager.getRingerMode();
        int vibrateType = -999;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
            //noinspection deprecation
            vibrateType = audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
        //int vibrateWhenRinging;
        //if (android.os.Build.VERSION.SDK_INT < 23)    // Not working in Android M (exception)
        //    vibrateWhenRinging = Settings.System.getInt(context.getContentResolver(), "vibrate_when_ringing", 0);
        //else
        //    vibrateWhenRinging = Settings.System.getInt(context.getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, 0);

        PPApplication.logE("PPApplication.vibrationIsOn", "ringerMode="+ringerMode);
        PPApplication.logE("PPApplication.vibrationIsOn", "vibrateType="+vibrateType);
        //PPApplication.logE("PPApplication.vibrationIsOn", "vibrateWhenRinging="+vibrateWhenRinging);

        //noinspection deprecation
        return (ringerMode == AudioManager.RINGER_MODE_VIBRATE) ||
                (vibrateType == AudioManager.VIBRATE_SETTING_ON) ||
                (vibrateType == AudioManager.VIBRATE_SETTING_ONLY_SILENT);// ||
        //(vibrateWhenRinging == 1);
    }

    private static void setRingerMode(Context context, Profile profile, AudioManager audioManager, boolean firstCall, boolean forProfileActivation)
    {
        //PPApplication.logE("@@@ ActivateProfileHelper.setRingerMode", "audioM.ringerMode=" + audioManager.getRingerMode());

        int ringerMode;
        int zenMode;

        if (forProfileActivation) {
            if (profile._volumeRingerMode != 0) {
                setRingerMode(context, profile._volumeRingerMode);
                if ((profile._volumeRingerMode == Profile.RINGERMODE_ZENMODE) && (profile._volumeZenMode != 0))
                    setZenMode(context, profile._volumeZenMode);
            }
        }

        if (firstCall)
            return;

        ringerMode = getRingerMode(context);
        zenMode = getZenMode(context);

        PPApplication.logE("ActivateProfileHelper.setRingerMode", "ringerMode=" + ringerMode);
        PPApplication.logE("ActivateProfileHelper.setRingerMode", "zenMode=" + zenMode);

        if (forProfileActivation) {

            PPApplication.logE("ActivateProfileHelper.setRingerMode", "ringer mode change");

            switch (ringerMode) {
                case Profile.RINGERMODE_RING:
                    PPApplication.logE("ActivateProfileHelper.setRingerMode", "ringer mode=RING");
                    setZenMode(context, ZENMODE_ALL, audioManager, AudioManager.RINGER_MODE_NORMAL);
                    //audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL); not needed, called from setZenMode
                    try {
                        //noinspection deprecation
                        audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
                    } catch (Exception ignored) {
                    }
                    try {
                        //noinspection deprecation
                        audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
                    } catch (Exception ignored) {
                    }
                    setVibrateWhenRinging(context, null, 0);
                    break;
                case Profile.RINGERMODE_RING_AND_VIBRATE:
                    PPApplication.logE("ActivateProfileHelper.setRingerMode", "ringer mode=RING & VIBRATE");
                    setZenMode(context, ZENMODE_ALL, audioManager, AudioManager.RINGER_MODE_NORMAL);
                    //audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL); not needed, called from setZenMode
                    try {
                        //noinspection deprecation
                        audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_ON);
                    } catch (Exception ignored) {
                    }
                    try {
                        //noinspection deprecation
                        audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_ON);
                    } catch (Exception ignored) {
                    }
                    setVibrateWhenRinging(context, null, 1);
                    break;
                case Profile.RINGERMODE_VIBRATE:
                    PPApplication.logE("ActivateProfileHelper.setRingerMode", "ringer mode=VIBRATE");
                    setZenMode(context, ZENMODE_ALL, audioManager, AudioManager.RINGER_MODE_VIBRATE);
                    //audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE); not needed, called from setZenMode
                    try {
                        //noinspection deprecation
                        audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_ON);
                    } catch (Exception ignored) {
                    }
                    try {
                        //noinspection deprecation
                        audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_ON);
                    } catch (Exception ignored) {
                    }
                    setVibrateWhenRinging(context, null, 1);
                    break;
                case Profile.RINGERMODE_SILENT:
                    PPApplication.logE("ActivateProfileHelper.setRingerMode", "ringer mode=SILENT");
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        //setZenMode(ZENMODE_SILENT, audioManager, AudioManager.RINGER_MODE_SILENT);
                        setZenMode(context, ZENMODE_SILENT, audioManager, AudioManager.RINGER_MODE_NORMAL);
                    }
                    else {
                        setZenMode(context, ZENMODE_ALL, audioManager, AudioManager.RINGER_MODE_SILENT);
                        try {
                            //noinspection deprecation
                            audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
                        } catch (Exception ignored) {
                        }
                        try {
                            //noinspection deprecation
                            audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
                        } catch (Exception ignored) {
                        }
                    }
                    setVibrateWhenRinging(context, null, 0);
                    break;
                case Profile.RINGERMODE_ZENMODE:
                    PPApplication.logE("ActivateProfileHelper.setRingerMode", "ringer mode=ZEN MODE");
                    switch (zenMode) {
                        case Profile.ZENMODE_ALL:
                            PPApplication.logE("ActivateProfileHelper.setRingerMode", "zen mode=ALL");
                            setZenMode(context, ZENMODE_ALL, audioManager, /*AudioManager.RINGER_MODE_NORMAL*/profile._ringerModeForZenMode);
                            setVibrateWhenRinging(context, profile, -1);
                            break;
                        case Profile.ZENMODE_PRIORITY:
                            PPApplication.logE("ActivateProfileHelper.setRingerMode", "zen mode=PRIORITY");
                            setZenMode(context, ZENMODE_PRIORITY, audioManager, /*AudioManager.RINGER_MODE_NORMAL*/profile._ringerModeForZenMode);
                            setVibrateWhenRinging(context, profile, -1);
                            break;
                        case Profile.ZENMODE_NONE:
                            PPApplication.logE("ActivateProfileHelper.setRingerMode", "zen mode=NONE");
                            // must be set to ALL and after to NONE
                            // without this, duplicate set this zen mode not working
                            setZenMode(context, ZENMODE_ALL, audioManager, AudioManager.RINGER_MODE_NORMAL);
                            //try { Thread.sleep(1000); } catch (InterruptedException e) { }
                            //SystemClock.sleep(1000);
                            PPApplication.sleep(1000);
                            setZenMode(context, ZENMODE_NONE, audioManager, /*AudioManager.RINGER_MODE_NORMAL*/profile._ringerModeForZenMode);
                            break;
                        case Profile.ZENMODE_ALL_AND_VIBRATE:
                            PPApplication.logE("ActivateProfileHelper.setRingerMode", "zen mode=ALL & VIBRATE");
                            setZenMode(context, ZENMODE_ALL, audioManager, AudioManager.RINGER_MODE_VIBRATE);
                            setVibrateWhenRinging(context, null, 1);
                            break;
                        case Profile.ZENMODE_PRIORITY_AND_VIBRATE:
                            PPApplication.logE("ActivateProfileHelper.setRingerMode", "zen mode=PRIORITY & VIBRATE");
                            setZenMode(context, ZENMODE_PRIORITY, audioManager, AudioManager.RINGER_MODE_VIBRATE);
                            setVibrateWhenRinging(context, null, 1);
                            break;
                        case Profile.ZENMODE_ALARMS:
                            PPApplication.logE("ActivateProfileHelper.setRingerMode", "zen mode=ALARMS");
                            // must be set to ALL and after to ALARMS
                            // without this, duplicate set this zen mode not working
                            setZenMode(context, ZENMODE_ALL, audioManager, AudioManager.RINGER_MODE_NORMAL);
                            //try { Thread.sleep(1000); } catch (InterruptedException e) { }
                            //SystemClock.sleep(1000);
                            PPApplication.sleep(1000);
                            setZenMode(context, ZENMODE_ALARMS, audioManager, /*AudioManager.RINGER_MODE_NORMAL*/profile._ringerModeForZenMode);
                            break;
                    }
                    break;
            }
        }
    }

    private static void executeForWallpaper(final Profile profile, final Context context) {
        if (profile._deviceWallpaperChange == 1)
        {
            final Context appContext = context.getApplicationContext();
            PPApplication.startHandlerThreadWallpaper();
            final Handler handler = new Handler(PPApplication.handlerThreadWallpaper.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = null;
                    if (powerManager != null) {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivateProfileHelper.executeForWallpaper");
                        wakeLock.acquire(10 * 60 * 1000);
                    }

                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                    if (wm != null) {
                        Display display = wm.getDefaultDisplay();
                        //if (android.os.Build.VERSION.SDK_INT >= 17)
                            display.getRealMetrics(displayMetrics);
                        //else
                        //    display.getMetrics(displayMetrics);
                        int height = displayMetrics.heightPixels;
                        int width = displayMetrics.widthPixels;
                        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            //noinspection SuspiciousNameCombination
                            height = displayMetrics.widthPixels;
                            //noinspection SuspiciousNameCombination
                            width = displayMetrics.heightPixels;
                        }
                        //Log.d("ActivateProfileHelper.executeForWallpaper", "height="+height);
                        //Log.d("ActivateProfileHelper.executeForWallpaper", "width="+width);
                        // for lock screen no double width
                        if ((android.os.Build.VERSION.SDK_INT < 24) || (profile._deviceWallpaperFor != 2))
                            width = width << 1; // best wallpaper width is twice screen width

                        Bitmap decodedSampleBitmap = BitmapManipulator.resampleBitmapUri(profile._deviceWallpaper, width, height, false, true, context);
                        if (decodedSampleBitmap != null) {
                            // set wallpaper
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= 24) {
                                    int flags = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
                                    Rect visibleCropHint = null;
                                    if (profile._deviceWallpaperFor == 1)
                                        flags = WallpaperManager.FLAG_SYSTEM;
                                    if (profile._deviceWallpaperFor == 2) {
                                        flags = WallpaperManager.FLAG_LOCK;
                                        int left = 0;
                                        int right = decodedSampleBitmap.getWidth();
                                        if (decodedSampleBitmap.getWidth() > width) {
                                            left = (decodedSampleBitmap.getWidth() / 2) - (width / 2);
                                            right = (decodedSampleBitmap.getWidth() / 2) + (width / 2);
                                        }
                                        visibleCropHint = new Rect(left, 0, right, decodedSampleBitmap.getHeight());
                                    }
                                    //noinspection WrongConstant
                                    wallpaperManager.setBitmap(decodedSampleBitmap, visibleCropHint, true, flags);
                                } else
                                    wallpaperManager.setBitmap(decodedSampleBitmap);
                            } catch (IOException e) {
                                Log.e("ActivateProfileHelper.executeForWallpaper", Log.getStackTraceString(e));
                            }
                        }
                    }

                    if ((wakeLock != null) && wakeLock.isHeld()) {
                        try {
                            wakeLock.release();
                        } catch (Exception ignored) {}
                    }
                }
            });
        }
    }

    // not working, returns only calling process :-/
    // http://stackoverflow.com/questions/30619349/android-5-1-1-and-above-getrunningappprocesses-returns-my-application-packag
    /*private boolean isRunning(List<ActivityManager.RunningAppProcessInfo> procInfos, String packageName) {
        PPApplication.logE("ActivateProfileHelper.executeForRunApplications", "procInfos.size()="+procInfos.size());
        for(int i = 0; i < procInfos.size(); i++)
        {
            ActivityManager.RunningAppProcessInfo procInfo = procInfos.get(i);
            PPApplication.logE("ActivateProfileHelper.executeForRunApplications", "procInfo.processName="+procInfo.processName);
            if (procInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                PPApplication.logE("ActivateProfileHelper.executeForRunApplications", "procInfo.importance=IMPORTANCE_FOREGROUND");
                for (String pkgName : procInfo.pkgList) {
                    PPApplication.logE("ActivateProfileHelper.executeForRunApplications", "pkgName="+pkgName);
                    if (pkgName.equals(packageName))
                        return true;
                }
            }
        }
        return false;
    }*/

    private static void executeForRunApplications(final Profile profile, final Context context) {
        if (profile._deviceRunApplicationChange == 1)
        {
            final Context appContext = context.getApplicationContext();
            PPApplication.startHandlerThreadRunApplication();
            final Handler handler = new Handler(PPApplication.handlerThreadRunApplication.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {

                    PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = null;
                    if (powerManager != null) {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivateProfileHelper.executeForRunApplications");
                        wakeLock.acquire(10 * 60 * 1000);
                    }

                    String[] splits = profile._deviceRunApplicationPackageName.split("\\|");
                    Intent intent;
                    PackageManager packageManager = context.getPackageManager();

                    //ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    //List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();

                    for (String split : splits) {
                        //Log.d("ActivateProfileHelper.executeForRunApplications","app data="+splits[i]);
                        int startApplicationDelay = ApplicationsCache.getStartApplicationDelay(split);
                        if (ApplicationsCache.getStartApplicationDelay(split) > 0) {
                            RunApplicationWithDelayBroadcastReceiver.setDelayAlarm(context, startApplicationDelay, split);
                        }
                        else {
                            if (!ApplicationsCache.isShortcut(split)) {
                                //Log.d("ActivateProfileHelper.executeForRunApplications","no shortcut");
                                String packageName = ApplicationsCache.getPackageName(split);
                                intent = packageManager.getLaunchIntentForPackage(packageName);
                                if (intent != null) {
                                    //if (!isRunning(procInfos, packageName)) {
                                    //    PPApplication.logE("ActivateProfileHelper.executeForRunApplications", packageName+": not running");
                                    //Log.d("ActivateProfileHelper.executeForRunApplications","intent="+intent);
                                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    try {
                                        context.startActivity(intent);
                                        //try { Thread.sleep(1000); } catch (InterruptedException e) { }
                                        //SystemClock.sleep(1000);
                                        PPApplication.sleep(1000);
                                    } catch (Exception ignored) {
                                    }
                                    //}
                                    //else
                                    //    PPApplication.logE("ActivateProfileHelper.executeForRunApplications", packageName+": running");
                                }
                            } else {
                                //Log.d("ActivateProfileHelper.executeForRunApplications","shortcut");
                                long shortcutId = ApplicationsCache.getShortcutId(split);
                                //Log.d("ActivateProfileHelper.executeForRunApplications","shortcutId="+shortcutId);
                                if (shortcutId > 0) {
                                    //Shortcut shortcut = dataWrapper.getDatabaseHandler().getShortcut(shortcutId);
                                    Shortcut shortcut = DatabaseHandler.getInstance(context).getShortcut(shortcutId);
                                    if (shortcut != null) {
                                        try {
                                            intent = Intent.parseUri(shortcut._intent, 0);
                                            if (intent != null) {
                                                //String packageName = intent.getPackage();
                                                //if (!isRunning(procInfos, packageName)) {
                                                //    PPApplication.logE("ActivateProfileHelper.executeForRunApplications", packageName + ": not running");
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                //Log.d("ActivateProfileHelper.executeForRunApplications","intent="+intent);
                                                try {
                                                    context.startActivity(intent);
                                                    //try { Thread.sleep(1000); } catch (InterruptedException e) { }
                                                    //SystemClock.sleep(1000);
                                                    PPApplication.sleep(1000);
                                                } catch (Exception ignored) {
                                                }
                                                //} else
                                                //    PPApplication.logE("ActivateProfileHelper.executeForRunApplications", packageName + ": running");
                                            }
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if ((wakeLock != null) && wakeLock.isHeld()) {
                        try {
                            wakeLock.release();
                        } catch (Exception ignored) {}
                    }
                }
            });
        }
    }

    private static void executeForForceStopApplications(final Profile profile, Context context) {
        if (PPApplication.startedOnBoot)
            // not force stop applications after boot
            return;

        if (profile._lockDevice != 0)
            // not force stop if profile has lock device enabled
            return;

        String applications = profile._deviceForceStopApplicationPackageName;
        if (!(applications.isEmpty() || (applications.equals("-")))) {
            Intent intent = new Intent(PPApplication.ACTION_FORCE_STOP_APPLICATIONS_START);
            intent.putExtra(PPApplication.EXTRA_PROFILE_ID, profile._id);
            intent.putExtra(PPApplication.EXTRA_APPLICATIONS, applications);
            context.sendBroadcast(intent, PPApplication.ACCESSIBILITY_SERVICE_PERMISSION);
        }
    }

    private static void executeRootForAdaptiveBrightness(final Profile profile, final Context context) {
        /* not working (private secure settings) :-/
        if (Permissions.hasPermission(appContext, Manifest.permission.WRITE_SECURE_SETTINGS)) {
            Settings.System.putFloat(appContext.getContentResolver(), ADAPTIVE_BRIGHTNESS_SETTING_NAME,
                    profile.getDeviceBrightnessAdaptiveValue(appContext));
        }
        else {*/
        final Context appContext = context.getApplicationContext();
        PPApplication.startHandlerThreadAdaptiveBrightness();
        final Handler handler = new Handler(PPApplication.handlerThreadAdaptiveBrightness.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

                PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivateProfileHelper.executeRootForAdaptiveBrightness");
                    wakeLock.acquire(10 * 60 * 1000);
                }

                if (PPApplication.isRooted() && PPApplication.settingsBinaryExists()) {
                    synchronized (PPApplication.rootMutex) {
                        String command1 = "settings put system " + ADAPTIVE_BRIGHTNESS_SETTING_NAME + " " +
                                Float.toString(profile.getDeviceBrightnessAdaptiveValue(context));
                        //if (PPApplication.isSELinuxEnforcing())
                        //	command1 = PPApplication.getSELinuxEnforceCommand(command1, Shell.ShellContext.SYSTEM_APP);
                        Command command = new Command(0, false, command1); //, command2);
                        try {
                            RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                            PPApplication.commandWait(command);
                        } catch (Exception e) {
                            Log.e("ActivateProfileHelper.execute", Log.getStackTraceString(e));
                        }
                    }
                }

                if ((wakeLock != null) && wakeLock.isHeld()) {
                    try {
                        wakeLock.release();
                    } catch (Exception ignored) {}
                }
            }
        });
        //}
    }

    static void executeForInteractivePreferences(final Profile profile, final Context context) {
        if (profile == null)
            return;

        if (profile._deviceRunApplicationChange == 1)
        {
            executeForRunApplications(profile, context);
        }

        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_MOBILE_DATA_PREFS, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
        {
            if (profile._deviceMobileDataPrefs == 1)
            {
                if ((pm != null) && pm.isScreenOn() && (myKM != null) && !myKM.isKeyguardLocked()) {
                    boolean ok = true;
                    try {
                        Intent intent = new Intent(Intent.ACTION_MAIN, null);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        ok = false;
                    }
                    if (!ok) {
                        ok = true;
                        try {
                            final Intent intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            final ComponentName componentName = new ComponentName("com.android.phone", "com.android.phone.Settings");
                            intent.setComponent(componentName);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            ok = false;
                        }
                    }
                    if (!ok) {
                        try {
                            final Intent intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } catch (Exception ignored) {
                        }
                    }
                }
                else {
                    boolean ok = false;
                    Intent intent = new Intent(Intent.ACTION_MAIN, null);
                    intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
                    if (GlobalGUIRoutines.activityIntentExists(intent, context))
                        ok = true;
                    if (!ok) {
                        intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
                        intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.Settings"));
                        if (GlobalGUIRoutines.activityIntentExists(intent, context))
                            ok = true;
                    }
                    if (!ok) {
                        intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
                        if (GlobalGUIRoutines.activityIntentExists(intent, context))
                            ok = true;
                    }
                    if (ok) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        String title = context.getString(R.string.profile_activation_interactive_preference_notification_title) + " " + profile._name;
                        String text = context.getString(R.string.profile_activation_interactive_preference_notification_text) + " " +
                                context.getString(R.string.profile_preferences_deviceMobileDataPrefs);
                        showNotificationForInteractiveParameters(context, title, text, intent, PPApplication.PROFILE_ACTIVATION_MOBILE_DATA_PREFS_NOTIFICATION_ID);
                    }
                }
            }
        }

        if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_NETWORK_TYPE_PREFS, context).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
        {
            if (profile._deviceNetworkTypePrefs == 1)
            {
                if ((pm != null) && pm.isScreenOn() && (myKM != null) && !myKM.isKeyguardLocked()) {
                    try {
                        final Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception ignored) {
                    }
                }
                else {
                    Intent intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    String title = context.getString(R.string.profile_activation_interactive_preference_notification_title) + " " + profile._name;
                    String text = context.getString(R.string.profile_activation_interactive_preference_notification_text) + " " +
                            context.getString(R.string.profile_preferences_deviceNetworkTypePrefs);
                    showNotificationForInteractiveParameters(context, title, text, intent, PPApplication.PROFILE_ACTIVATION_NETWORK_TYPE_PREFS_NOTIFICATION_ID);
                }
            }
        }

        //if (PPApplication.hardwareCheck(PPApplication.PREF_PROFILE_DEVICE_GPS, context))
        //{  No check only GPS
        if (profile._deviceLocationServicePrefs == 1)
        {
            if ((pm != null) && pm.isScreenOn() && (myKM != null) && !myKM.isKeyguardLocked()) {
                try {
                    final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception ignored) {
                }
            }
            else {
                final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                if (GlobalGUIRoutines.activityIntentExists(intent, context)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    String title = context.getString(R.string.profile_activation_interactive_preference_notification_title) + " " + profile._name;
                    String text = context.getString(R.string.profile_activation_interactive_preference_notification_text) + " " +
                            context.getString(R.string.profile_preferences_deviceLocationServicePrefs);
                    showNotificationForInteractiveParameters(context, title, text, intent, PPApplication.PROFILE_ACTIVATION_LOCATION_PREFS_NOTIFICATION_ID);
                }
            }
        }
        //}
        if (profile._deviceWiFiAPPrefs == 1) {
            if ((pm != null) && pm.isScreenOn() && (myKM != null) && !myKM.isKeyguardLocked()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_MAIN, null);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
                    context.startActivity(intent);
                } catch (Exception ignored) {
                }
            }
            else {
                Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
                if (GlobalGUIRoutines.activityIntentExists(intent, context)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    String title = context.getString(R.string.profile_activation_interactive_preference_notification_title) + " " + profile._name;
                    String text = context.getString(R.string.profile_activation_interactive_preference_notification_text) + " " +
                            context.getString(R.string.profile_preferences_deviceWiFiAPPrefs);
                    showNotificationForInteractiveParameters(context, title, text, intent, PPApplication.PROFILE_ACTIVATION_WIFI_AP_PREFS_NOTIFICATION_ID);
                }
            }
        }
    }

    static void execute(final Context context, final Profile _profile/*, boolean merged, *//*boolean _interactive*/)
    {
        PPApplication.logE("##### ActivateProfileHelper.execute", "xxx");

        // unlink ring and notifications - it is @Hide :-(
        //Settings.System.putInt(context.getContentResolver(), Settings.System.NOTIFICATIONS_USE_RING_VOLUME, 0);

        final Profile profile = Profile.getMappedProfile(_profile, context);

        // setup volume
        ActivateProfileHelper.executeForVolumes(profile, true, context);

        // set vibration on touch
        if (Permissions.checkProfileVibrationOnTouch(context, profile, null)) {
            switch (profile._vibrationOnTouch) {
                case 1:
                    Settings.System.putInt(context.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1);
                    //Settings.System.putInt(context.getContentResolver(), Settings.Global.CHARGING_SOUNDS_ENABLED, 1);
                    // Settings.System.DTMF_TONE_WHEN_DIALING - working
                    // Settings.System.SOUND_EFFECTS_ENABLED - working
                    // Settings.System.LOCKSCREEN_SOUNDS_ENABLED - private secure settings :-(
                    // Settings.Global.CHARGING_SOUNDS_ENABLED - java.lang.IllegalArgumentException: You cannot keep your settings in the secure settings. :-/
                    //                                           (G1) not working :-/
                    break;
                case 2:
                    Settings.System.putInt(context.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
                    //Settings.System.putInt(context.getContentResolver(), Settings.Global.CHARGING_SOUNDS_ENABLED, 0);
                    break;
            }
        }
        // set dtmf tone when dialing
        if (Permissions.checkProfileDtmfToneWhenDialing(context, profile, null)) {
            switch (profile._dtmfToneWhenDialing) {
                case 1:
                    Settings.System.putInt(context.getContentResolver(), Settings.System.DTMF_TONE_WHEN_DIALING, 1);
                    break;
                case 2:
                    Settings.System.putInt(context.getContentResolver(), Settings.System.DTMF_TONE_WHEN_DIALING, 0);
                    break;
            }
        }
        // set sound on touch
        if (Permissions.checkProfileSoundOnTouch(context, profile, null)) {
            switch (profile._soundOnTouch) {
                case 1:
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1);
                    break;
                case 2:
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
                    break;
            }
        }

        //// setup radio preferences
        ActivateProfileHelper.executeForRadios(profile, context);

        // setup auto-sync
        try {
            boolean _isAutoSync = ContentResolver.getMasterSyncAutomatically();
            boolean _setAutoSync = false;
            switch (profile._deviceAutoSync) {
                case 1:
                    if (!_isAutoSync) {
                        _isAutoSync = true;
                        _setAutoSync = true;
                    }
                    break;
                case 2:
                    if (_isAutoSync) {
                        _isAutoSync = false;
                        _setAutoSync = true;
                    }
                    break;
                case 3:
                    _isAutoSync = !_isAutoSync;
                    _setAutoSync = true;
                    break;
            }
            if (_setAutoSync)
                ContentResolver.setMasterSyncAutomatically(_isAutoSync);
        } catch (Exception ignored) {} // fixed DeadObjectException

        // screen timeout
        if (Permissions.checkProfileScreenTimeout(context, profile, null)) {
            PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
            if ((pm != null) && pm.isScreenOn()) {
                //Log.d("ActivateProfileHelper.execute","screen on");
                if (PPApplication.screenTimeoutHandler != null) {
                    PPApplication.screenTimeoutHandler.post(new Runnable() {
                        public void run() {
                            PPApplication.logE("ActivateProfileHelper.execute","screenTimeoutHandler");
                            setScreenTimeout(profile._deviceScreenTimeout, context);
                        }
                    });
                }// else
                //    setScreenTimeout(profile._deviceScreenTimeout);
            }
            else {
                //Log.d("ActivateProfileHelper.execute","screen off");
                setActivatedProfileScreenTimeout(context, profile._deviceScreenTimeout);
            }
        }
        //else
        //    PPApplication.setActivatedProfileScreenTimeout(context, 0);

        // on/off lock screen
        //PPApplication.logE("$$$ ActivateProfileHelper.execute","keyguard");
        boolean setLockScreen = false;
        switch (profile._deviceKeyguard) {
            case 1:
                // enable lock screen
                PPApplication.logE("$$$ ActivateProfileHelper.execute","keyguard=ON");
                setLockScreenDisabled(context, false);
                setLockScreen = true;
                break;
            case 2:
                // disable lock screen
                PPApplication.logE("$$$ ActivateProfileHelper.execute","keyguard=OFF");
                setLockScreenDisabled(context, true);
                setLockScreen = true;
                break;
        }
        if (setLockScreen) {
            boolean isScreenOn;
            //if (android.os.Build.VERSION.SDK_INT >= 20)
            //{
            //	Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            //	isScreenOn = display.getState() != Display.STATE_OFF;
            //}
            //else
            //{
            PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
            if (pm != null) {
                isScreenOn = pm.isScreenOn();
                //}
                PPApplication.logE("$$$ ActivateProfileHelper.execute", "isScreenOn=" + isScreenOn);
                boolean keyguardShowing;
                KeyguardManager kgMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if (kgMgr != null) {
                    keyguardShowing = kgMgr.isKeyguardLocked();
                    PPApplication.logE("$$$ ActivateProfileHelper.execute", "keyguardShowing=" + keyguardShowing);

                    if (isScreenOn && !keyguardShowing) {
                        try {
                            // start PhoneProfilesService
                            //PPApplication.firstStartServiceStarted = false;
                            Intent serviceIntent = new Intent(context, PhoneProfilesService.class);
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_SWITCH_KEYGUARD, true);
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
                            PPApplication.startPPService(context, serviceIntent);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        // setup display brightness
        if (Permissions.checkProfileScreenBrightness(context, profile, null)) {
            if (profile.getDeviceBrightnessChange()) {
                PPApplication.logE("----- ActivateProfileHelper.execute", "set brightness: profile=" + profile._name);
                PPApplication.logE("----- ActivateProfileHelper.execute", "set brightness: _deviceBrightness=" + profile._deviceBrightness);

                if (profile.getDeviceBrightnessAutomatic()) {
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                    if (profile.getDeviceBrightnessChangeLevel()) {
                        Settings.System.putInt(context.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS,
                                profile.getDeviceBrightnessManualValue(context));
                        if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_ADAPTIVE_BRIGHTNESS, context).allowed
                                == PreferenceAllowed.PREFERENCE_ALLOWED) {
                            if (android.os.Build.VERSION.SDK_INT < 23)    // Not working in Android M (exception)
                                Settings.System.putFloat(context.getContentResolver(),
                                        ADAPTIVE_BRIGHTNESS_SETTING_NAME,
                                        profile.getDeviceBrightnessAdaptiveValue(context));
                            else {
                                try {
                                    Settings.System.putFloat(context.getContentResolver(),
                                            ADAPTIVE_BRIGHTNESS_SETTING_NAME,
                                            profile.getDeviceBrightnessAdaptiveValue(context));
                                } catch (Exception ee) {
                                    // run service for execute radios
                                    ActivateProfileHelper.executeRootForAdaptiveBrightness(profile, context);
                                }
                            }
                        }
                    }
                } else {
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    if (profile.getDeviceBrightnessChangeLevel()) {
                        Settings.System.putInt(context.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS,
                                profile.getDeviceBrightnessManualValue(context));
                    }
                }

                if (PPApplication.brightnessHandler != null) {
                    PPApplication.brightnessHandler.post(new Runnable() {
                        public void run() {
                            PPApplication.logE("ActivateProfileHelper.execute","brightnessHandler");
                            createBrightnessView(profile, context);
                        }
                    });
                }// else
                //    createBrightnessView(context);
            }
        }

        // setup rotation
        if (Permissions.checkProfileAutoRotation(context, profile, null)) {
            switch (profile._deviceAutoRotate) {
                case 1:
                    // set autorotate on
                    Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
                    //Settings.System.putInt(context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_0);
                    break;
                case 6:
                    // set autorotate off
                    Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                    //Settings.System.putInt(context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_0);
                    break;
                case 2:
                    // set autorotate off
                    // degree 0
                    Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                    Settings.System.putInt(context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_0);
                    break;
                case 3:
                    // set autorotate off
                    // degree 90
                    Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                    Settings.System.putInt(context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_90);
                    break;
                case 4:
                    // set autorotate off
                    // degree 180
                    Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                    Settings.System.putInt(context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_180);
                    break;
                case 5:
                    // set autorotate off
                    // degree 270
                    Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                    Settings.System.putInt(context.getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_270);
                    break;
            }
        }

        // set notification led
        if (profile._notificationLed != 0) {
            //if (Permissions.checkProfileNotificationLed(context, profile)) { not needed for Android 6+, because root is required
            switch (profile._notificationLed) {
                case 1:
                    setNotificationLed(context, 1);
                    break;
                case 2:
                    setNotificationLed(context, 0);
                    break;
            }
            //}
        }

        // setup wallpaper
        if (Permissions.checkProfileWallpaper(context, profile, null)) {
            if (profile._deviceWallpaperChange == 1) {
                ActivateProfileHelper.executeForWallpaper(profile, context);
            }
        }

        // set power save mode
        ActivateProfileHelper.setPowerSaveMode(profile, context);

        if (Permissions.checkProfileLockDevice(context, profile, null)) {
            if (profile._lockDevice != 0) {
                boolean keyguardLocked;
                KeyguardManager kgMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if (kgMgr != null) {
                    keyguardLocked = kgMgr.isKeyguardLocked();
                    PPApplication.logE("---$$$ ActivateProfileHelper.execute", "keyguardLocked=" + keyguardLocked);
                    if (!keyguardLocked) {
                        ActivateProfileHelper.lockDevice(profile, context);
                    }
                }
            }
        }

        // enable/disable scanners
        if (profile._applicationDisableWifiScanning != 0) {
            ApplicationPreferences.getSharedPreferences(context);
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_WIFI_ENABLE_SCANNING, profile._applicationDisableWifiScanning == 2);
            if (profile._applicationDisableWifiScanning == 1)
                editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_WIFI_DISABLED_SCANNING_BY_PROFILE, true);
            editor.apply();
            PPApplication.restartWifiScanner(context, false);
        }
        if (profile._applicationDisableBluetoothScanning != 0) {
            ApplicationPreferences.getSharedPreferences(context);
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_BLUETOOTH_ENABLE_SCANNING, profile._applicationDisableBluetoothScanning == 2);
            if (profile._applicationDisableBluetoothScanning == 1)
                editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_BLUETOOTH_DISABLED_SCANNING_BY_PROFILE, true);
            editor.apply();
            PPApplication.restartBluetoothScanner(context, false);
        }
        if (profile._applicationDisableLocationScanning != 0) {
            ApplicationPreferences.getSharedPreferences(context);
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_LOCATION_ENABLE_SCANNING, profile._applicationDisableLocationScanning == 2);
            if (profile._applicationDisableLocationScanning == 1)
                editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_LOCATION_DISABLED_SCANNING_BY_PROFILE, true);
            editor.apply();
            PPApplication.restartGeofenceScanner(context, false);
        }
        if (profile._applicationDisableMobileCellScanning != 0) {
            ApplicationPreferences.getSharedPreferences(context);
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_MOBILE_CELL_ENABLE_SCANNING, profile._applicationDisableMobileCellScanning == 2);
            if (profile._applicationDisableMobileCellScanning == 1)
                editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_MOBILE_CELL_DISABLED_SCANNING_BY_PROFILE, true);
            editor.apply();
            PPApplication.restartPhoneStateScanner(context, false);
        }
        if (profile._applicationDisableOrientationScanning != 0) {
            ApplicationPreferences.getSharedPreferences(context);
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_ORIENTATION_ENABLE_SCANNING, profile._applicationDisableOrientationScanning == 2);
            if (profile._applicationDisableOrientationScanning == 1)
                editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_ORIENTATION_DISABLED_SCANNING_BY_PROFILE, true);
            editor.apply();
            PPApplication.restartOrientationScanner(context);
        }

        // set heads-up notifications
        if (profile._headsUpNotifications != 0) {
            switch (profile._headsUpNotifications) {
                case 1:
                    setHeadsUpNotifications(context, 1);
                    break;
                case 2:
                    setHeadsUpNotifications(context, 0);
                    break;
            }
        }

        /*
        // set screen night mode
        if (profile._screenNightMode != 0) {
            setScreenNightMode(context, profile._screenNightMode);
        }
        */

        // close all applications
        if (profile._deviceCloseAllApplications == 1) {
            if (!PPApplication.startedOnBoot) {
                try {
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(startMain);
                } catch (Exception e) {
                    Log.e("ActivateProfileHelper.execute", Log.getStackTraceString(e));
                }
            }
        }

        if ((profile._deviceForceStopApplicationChange == 1) &&
                AccessibilityServiceBroadcastReceiver.isEnabled(context, PPApplication.VERSION_CODE_EXTENDER_2_0))
        {
            // executeForInteractivePreferences() is called from broadcast receiver AccessibilityServiceBroadcastReceiver
            ActivateProfileHelper.executeForForceStopApplications(profile, context);
        }
        else
            executeForInteractivePreferences(profile, context);


        //throw new RuntimeException("test Crashlytics + TopExceptionHandler");
    }

    private static void showNotificationForInteractiveParameters(Context context, String title, String text, Intent intent, int notificationId) {
        String nTitle = title;
        String nText = text;
        if (android.os.Build.VERSION.SDK_INT < 24) {
            nTitle = context.getString(R.string.app_name);
            nText = title+": "+text;
        }
        PPApplication.createInformationNotificationChannel(context);
        NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(context, PPApplication.INFORMATION_NOTIFICATION_CHANNEL)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setSmallIcon(R.drawable.ic_exclamation_notify) // notification icon
                .setContentTitle(nTitle) // title for notification
                .setContentText(nText) // message for notification
                .setAutoCancel(true); // clear notification after click
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(nText));
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        if (android.os.Build.VERSION.SDK_INT >= 21)
        {
            mBuilder.setCategory(Notification.CATEGORY_RECOMMENDATION);
            mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null)
            mNotificationManager.notify(notificationId, mBuilder.build());
    }

    static void setScreenTimeout(int screenTimeout, Context context) {
        PPApplication.logE("ActivateProfileHelper.setScreenTimeout", "xxx");
        disableScreenTimeoutInternalChange = true;
        //Log.d("ActivateProfileHelper.setScreenTimeout", "current="+Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0));
        switch (screenTimeout) {
            case 1:
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 15000;
                else
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 15000);
                break;
            case 2:
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 30000;
                else
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 30000);
                break;
            case 3:
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 60000;
                else
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 60000);
                break;
            case 4:
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 120000;
                else
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 120000);
                break;
            case 5:
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 600000;
                else
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 600000);
                break;
            case 6:
                //2147483647 = Integer.MAX_VALUE
                //18000000   = 5 hours
                //86400000   = 24 hours
                //43200000   = 12 hours
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 86400000;
                else
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 86400000);
                break;
            case 7:
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 300000;
                else
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 300000);
                break;
            case 8:
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 86400000; //1800000;
                else
                    createScreenTimeoutAlwaysOnView(context);
                break;
            case 9:
                removeScreenTimeoutAlwaysOnView(context);
                if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().lockDeviceActivity != null))
                    // in LockDeviceActivity.onDestroy() will be used this value to revert back system screen timeout
                    PhoneProfilesService.getInstance().screenTimeoutBeforeDeviceLock = 1800000;
                else
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1800000);
                break;
        }
        setActivatedProfileScreenTimeout(context, 0);
        /*
        PPApplication.startHandlerThread("ActivateProfileHelper.setScreenTimeout");
        final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PPApplication.logE("ActivateProfileHelper.setScreenTimeout", "disable screen timeout internal change");
                disableScreenTimeoutInternalChange = false;
            }
        }, 3000);
        */
        PostDelayedBroadcastReceiver.setAlarm(
                PostDelayedBroadcastReceiver.ACTION_DISABLE_SCREEN_TIMEOUT_INTERNAL_CHANGE_TO_FALSE, 3);
    }

    private static void createScreenTimeoutAlwaysOnView(Context context)
    {
        removeScreenTimeoutAlwaysOnView(context);

        if (PhoneProfilesService.getInstance() != null) {
            final Context appContext = context.getApplicationContext();

            // Put 30 minutes screen timeout. Required for SettingsContentObserver.OnChange to call removeScreenTimeoutAlwaysOnView
            // when user change system setting.
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 86400000);

            WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                int type;
                if (android.os.Build.VERSION.SDK_INT < 25)
                    type = WindowManager.LayoutParams.TYPE_TOAST;
                else if (android.os.Build.VERSION.SDK_INT < 26)
                    type = LayoutParams.TYPE_SYSTEM_OVERLAY; // add show ACTION_MANAGE_OVERLAY_PERMISSION to Permissions app Settings
                else
                    type = LayoutParams.TYPE_APPLICATION_OVERLAY; // add show ACTION_MANAGE_OVERLAY_PERMISSION to Permissions app Settings
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        1, 1,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | /*WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |*/ WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        PixelFormat.TRANSLUCENT
                );
                /*if (android.os.Build.VERSION.SDK_INT < 17)
                    params.gravity = Gravity.RIGHT | Gravity.TOP;
                else
                    params.gravity = Gravity.END | Gravity.TOP;*/
                PhoneProfilesService.getInstance().keepScreenOnView = new BrightnessView(appContext);
                try {
                    windowManager.addView(PhoneProfilesService.getInstance().keepScreenOnView, params);
                } catch (Exception e) {
                    PhoneProfilesService.getInstance().keepScreenOnView = null;
                }
            }
        }
    }

    static void removeScreenTimeoutAlwaysOnView(Context context)
    {
        if (PhoneProfilesService.getInstance() != null) {
            if (PhoneProfilesService.getInstance().keepScreenOnView != null) {
                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (windowManager != null) {
                    try {
                        windowManager.removeView(PhoneProfilesService.getInstance().keepScreenOnView);
                    } catch (Exception ignored) {
                    }
                    PhoneProfilesService.getInstance().keepScreenOnView = null;
                }
            }
        }
    }

    @SuppressLint("RtlHardcoded")
    private static void createBrightnessView(Profile profile, Context context)
    {
        PPApplication.logE("ActivateProfileHelper.createBrightnessView", "xxx");

        if (PhoneProfilesService.getInstance() != null) {
            final Context appContext = context.getApplicationContext();

            WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                if (PhoneProfilesService.getInstance().brightnessView != null) {
                    try {
                        windowManager.removeView(PhoneProfilesService.getInstance().brightnessView);
                    } catch (Exception ignored) {
                    }
                    PhoneProfilesService.getInstance().brightnessView = null;
                }
                int type;
                if (android.os.Build.VERSION.SDK_INT < 25)
                    type = WindowManager.LayoutParams.TYPE_TOAST;
                else if (android.os.Build.VERSION.SDK_INT < 26)
                    type = LayoutParams.TYPE_SYSTEM_OVERLAY; // add show ACTION_MANAGE_OVERLAY_PERMISSION to Permissions app Settings
                else
                    type = LayoutParams.TYPE_APPLICATION_OVERLAY; // add show ACTION_MANAGE_OVERLAY_PERMISSION to Permissions app Settings
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        1, 1,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE /*| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE*/,
                        PixelFormat.TRANSLUCENT
                );
                if (profile.getDeviceBrightnessAutomatic() || (!profile.getDeviceBrightnessChangeLevel()))
                    params.screenBrightness = LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                else
                    params.screenBrightness = profile.getDeviceBrightnessManualValue(appContext) / (float) 255;
                PhoneProfilesService.getInstance().brightnessView = new BrightnessView(appContext);
                try {
                    windowManager.addView(PhoneProfilesService.getInstance().brightnessView, params);
                } catch (Exception e) {
                    PhoneProfilesService.getInstance().brightnessView = null;
                }

                /*
                final Handler handler = new Handler(appContext.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PPApplication.logE("ActivateProfileHelper.createBrightnessView", "remove brightness view");

                        WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
                        if (windowManager != null) {
                            if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().brightnessView != null)) {
                                try {
                                    windowManager.removeView(PhoneProfilesService.getInstance().brightnessView);
                                } catch (Exception ignored) {
                                }
                                PhoneProfilesService.getInstance().brightnessView = null;
                            }
                        }
                    }
                }, 5000);
                */
                PostDelayedBroadcastReceiver.setAlarm(
                        PostDelayedBroadcastReceiver.ACTION_REMOVE_BRIGHTNESS_VIEW,5);
            }
        }
    }

    static void removeBrightnessView(Context context) {
        if (PhoneProfilesService.getInstance() != null) {
            if (PhoneProfilesService.getInstance().brightnessView != null) {
                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (windowManager != null) {
                    try {
                        windowManager.removeView(PhoneProfilesService.getInstance().brightnessView);
                    } catch (Exception ignored) {
                    }
                    PhoneProfilesService.getInstance().brightnessView = null;
                }
            }
        }
    }

    static void updateGUI(Context context, boolean alsoEditor)
    {
        if (lockRefresh || EditorProfilesActivity.doImport)
            // no refresh widgets
            return;

        // icon widget
        try {
            Intent intent = new Intent(context, IconWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int ids[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, IconWidgetProvider.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);
        } catch (Exception ignored) {}

        // one row widget
        try {
            Intent intent4 = new Intent(context, OneRowWidgetProvider.class);
            intent4.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int ids4[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, OneRowWidgetProvider.class));
            intent4.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids4);
            context.sendBroadcast(intent4);
        } catch (Exception ignored) {}

        // list widget
        try {
            Intent intent2 = new Intent(context, ProfileListWidgetProvider.class);
            intent2.setAction(ProfileListWidgetProvider.INTENT_REFRESH_LISTWIDGET);
            int ids2[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, ProfileListWidgetProvider.class));
            intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids2);
            context.sendBroadcast(intent2);
        } catch (Exception ignored) {}

        // dash clock extension
        Intent intent3 = new Intent("DashClockBroadcastReceiver");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent3);

        // activities
        Intent intent5 = new Intent("RefreshGUIBroadcastReceiver");
        intent5.putExtra(RefreshGUIBroadcastReceiver.EXTRA_REFRESH_ALSO_EDITOR, alsoEditor);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent5);

        // Samsung edge panel
        if ((PPApplication.sLook != null) && PPApplication.sLookCocktailPanelEnabled) {
            try {
                Intent intent2 = new Intent(context, SamsungEdgeProvider.class);
                intent2.setAction(SamsungEdgeProvider.INTENT_REFRESH_EDGEPANEL);
                context.sendBroadcast(intent2);
            } catch (Exception ignored) {
            }
        }
    }

    static boolean isAirplaneMode(Context context)
    {
        //if (android.os.Build.VERSION.SDK_INT >= 17)
            return Settings.Global.getInt(context.getContentResolver(), Global.AIRPLANE_MODE_ON, 0) != 0;
        //else
        //    return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private static void setAirplaneMode(/*Context context, */boolean mode)
    {
        //if (android.os.Build.VERSION.SDK_INT >= 17)
            setAirplaneMode_SDK17(/*context, */mode);
        //else
        //    setAirplaneMode_SDK8(context, mode);
    }

    static boolean isMobileData(Context context)
    {
        if (android.os.Build.VERSION.SDK_INT < 21)
        {
            ConnectivityManager connectivityManager = null;
            try {
                connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            } catch (Exception ignored) {
                // java.lang.NullPointerException: missing IConnectivityManager
                // Dual SIM?? Bug in Android ???
            }
            if (connectivityManager != null) {
                try {
                    //TODO Android P - used reflections!!
                    final Class<?> connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
                    final Method getMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
                    getMobileDataEnabledMethod.setAccessible(true);
                    return (Boolean) getMobileDataEnabledMethod.invoke(connectivityManager);
                } catch (Exception e) {
                    return false;
                }
            }
            else
                return false;
        }
        else
        if (android.os.Build.VERSION.SDK_INT < 22)
        {
            Method getDataEnabledMethod;
            Class<?> telephonyManagerClass;
            Object ITelephonyStub;
            Class<?> ITelephonyClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                try {
                    //TODO Android P - used reflections!!
                    telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                    Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                    getITelephonyMethod.setAccessible(true);
                    ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                    ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

                    getDataEnabledMethod = ITelephonyClass.getDeclaredMethod("getDataEnabled");

                    getDataEnabledMethod.setAccessible(true);

                    return (Boolean) getDataEnabledMethod.invoke(ITelephonyStub);

                } catch (Exception e) {
                    return false;
                }
            }
            else
                return false;
        }
        else
        {
            Method getDataEnabledMethod;
            Class<?> telephonyManagerClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                try {
                    //TODO Android P - used reflections!!
                    telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                    getDataEnabledMethod = telephonyManagerClass.getDeclaredMethod("getDataEnabled");
                    getDataEnabledMethod.setAccessible(true);

                    return (Boolean) getDataEnabledMethod.invoke(telephonyManager);

                } catch (Exception e) {
                    return false;
                }
            }
            else
                return false;
        }

    }

    static boolean canSetMobileData(Context context)
    {
        if (android.os.Build.VERSION.SDK_INT >= 22)
        {
            Class<?> telephonyManagerClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                try {
                    //TODO Android P - used reflections!!
                    telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                    Method getDataEnabledMethod = telephonyManagerClass.getDeclaredMethod("getDataEnabled");
                    getDataEnabledMethod.setAccessible(true);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            else
                return false;
        }
        else
        if (android.os.Build.VERSION.SDK_INT >= 21)
        {
            Class<?> telephonyManagerClass;

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                try {
                    //TODO Android P - used reflections!!
                    telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                    Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                    getITelephonyMethod.setAccessible(true);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            else
                return false;
        }
        else
        {
            ConnectivityManager connectivityManager = null;
            try {
                connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            } catch (Exception ignored) {
                // java.lang.NullPointerException: missing IConnectivityManager
                // Dual SIM?? Bug in Android ???
            }
            if (connectivityManager != null) {
                try {
                    //TODO Android P - used reflections!!
                    final Class<?> connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
                    final Method getMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
                    getMobileDataEnabledMethod.setAccessible(true);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            else
                return false;
        }
    }

    private static void setMobileData(Context context, boolean enable)
    {
        if (android.os.Build.VERSION.SDK_INT >= 21)
        {
            // adb shell pm grant sk.henrichg.phoneprofilesplus android.permission.MODIFY_PHONE_STATE
            // not working :-/
            if (Permissions.hasPermission(context, Manifest.permission.MODIFY_PHONE_STATE)) {
                if (android.os.Build.VERSION.SDK_INT == 21)
                {
                    Method dataConnSwitchMethod;
                    Class<?> telephonyManagerClass;
                    Object ITelephonyStub;
                    Class<?> ITelephonyClass;

                    TelephonyManager telephonyManager = (TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        try {
                            //TODO Android P - used reflections!!
                            telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                            Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                            getITelephonyMethod.setAccessible(true);
                            ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                            ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());
                            dataConnSwitchMethod = ITelephonyClass.getDeclaredMethod("setDataEnabled", Boolean.TYPE);

                            dataConnSwitchMethod.setAccessible(true);
                            dataConnSwitchMethod.invoke(ITelephonyStub, enable);

                        } catch (Exception ignored) {
                        }
                    }
                }
                else
                {
                    Method setDataEnabledMethod;
                    Class<?> telephonyManagerClass;

                    TelephonyManager telephonyManager = (TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        try {
                            //TODO Android P - used reflections!!
                            telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                            setDataEnabledMethod = telephonyManagerClass.getDeclaredMethod("setDataEnabled", Boolean.TYPE);
                            setDataEnabledMethod.setAccessible(true);

                            setDataEnabledMethod.invoke(telephonyManager, enable);

                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            else
            if (PPApplication.isRooted()/*PPApplication.isRootGranted()*/)
            {
                synchronized (PPApplication.rootMutex) {
                    String command1 = "svc data " + (enable ? "enable" : "disable");
                    PPApplication.logE("ActivateProfileHelper.setMobileData", "command=" + command1);
                    Command command = new Command(0, false, command1)/* {
                    @Override
                    public void commandOutput(int id, String line) {
                        super.commandOutput(id, line);
                        PPApplication.logE("ActivateProfileHelper.setMobileData","shell output="+line);
                    }

                    @Override
                    public void commandTerminated(int id, String reason) {
                        super.commandTerminated(id, reason);
                        PPApplication.logE("ActivateProfileHelper.setMobileData","terminated="+reason);
                    }

                    @Override
                    public void commandCompleted(int id, int exitCode) {
                        super.commandCompleted(id, exitCode);
                        PPApplication.logE("ActivateProfileHelper.setMobileData","completed="+exitCode);
                    }
                    }*/;
                    try {
                        RootTools.getShell(true, Shell.ShellContext.SHELL).add(command);
                        PPApplication.commandWait(command);
                        PPApplication.logE("ActivateProfileHelper.setMobileData", "after wait");
                    } catch (Exception e) {
                        Log.e("ActivateProfileHelper.setMobileData", Log.getStackTraceString(e));
                    }

                    /*
                    int state = 0;
                    try {
                        // Get the current state of the mobile network.
                        state = enable ? 1 : 0;
                        // Get the value of the "TRANSACTION_setDataEnabled" field.
                        String transactionCode = PPApplication.getTransactionCode(context, "TRANSACTION_setDataEnabled");
                        //Log.e("ActivateProfileHelper.setMobileData", "transactionCode="+transactionCode);
                        // Android 5.1+ (API 22) and later.
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                            //Log.e("ActivateProfileHelper.setMobileData", "dual SIM?");
                            SubscriptionManager mSubscriptionManager = SubscriptionManager.from(context);
                            // Loop through the subscription list i.e. SIM list.
                            for (int i = 0; i < mSubscriptionManager.getActiveSubscriptionInfoCountMax(); i++) {
                                if (transactionCode != null && transactionCode.length() > 0) {
                                    // Get the active subscription ID for a given SIM card.
                                    int subscriptionId = mSubscriptionManager.getActiveSubscriptionInfoList().get(i).getSubscriptionId();
                                    //Log.e("ActivateProfileHelper.setMobileData", "subscriptionId="+subscriptionId);
                                    String command1 = "service call phone " + transactionCode + " i32 " + subscriptionId + " i32 " + state;
                                    Command command = new Command(0, false, command1);
                                    try {
                                        RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                        commandWait(command);
                                    } catch (Exception e) {
                                        Log.e("ActivateProfileHelper.setMobileData", Log.getStackTraceString(e));
                                    }
                                }
                            }
                        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                            //Log.e("ActivateProfileHelper.setMobileData", "NO dual SIM?");
                            // Android 5.0 (API 21) only.
                            if (transactionCode != null && transactionCode.length() > 0) {
                                String command1 = "service call phone " + transactionCode + " i32 " + state;
                                Command command = new Command(0, false, command1);
                                try {
                                    RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                    commandWait(command);
                                } catch (Exception e) {
                                    Log.e("ActivateProfileHelper.setMobileData", Log.getStackTraceString(e));
                                }
                            }
                        }
                    } catch(Exception e) {
                        Log.e("ActivateProfileHelper.setMobileData", Log.getStackTraceString(e));
                    }
                    */
                }
            }
        }
        else
        {
            ConnectivityManager connectivityManager = null;
            try {
                connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            } catch (Exception ignored) {
                // java.lang.NullPointerException: missing IConnectivityManager
                // Dual SIM?? Bug in Android ???
            }
            if (connectivityManager != null) {
                boolean OK = false;
                try {
                    //TODO Android P - used reflections!!
                    final Class<?> connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
                    final Field iConnectivityManagerField = connectivityManagerClass.getDeclaredField("mService");
                    iConnectivityManagerField.setAccessible(true);
                    final Object iConnectivityManager = iConnectivityManagerField.get(connectivityManager);
                    final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
                    final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                    setMobileDataEnabledMethod.setAccessible(true);

                    setMobileDataEnabledMethod.invoke(iConnectivityManager, enable);

                    OK = true;

                } catch (Exception ignored) {
                }

                if (!OK) {
                    try {
                        //TODO Android P - used reflections!!
                        //noinspection JavaReflectionMemberAccess
                        @SuppressLint("PrivateApi")
                        Method setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);

                        setMobileDataEnabledMethod.setAccessible(true);
                        setMobileDataEnabledMethod.invoke(connectivityManager, enable);

                        //OK = true;

                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /*
    private int getPreferredNetworkType(Context context) {
        if (PPApplication.isRooted())
        {
            try {
                // Get the value of the "TRANSACTION_setPreferredNetworkType" field.
                String transactionCode = PPApplication.getTransactionCode(context, "TRANSACTION_getPreferredNetworkType");
                if (transactionCode != null && transactionCode.length() > 0) {
                    String command1 = "service call phone " + transactionCode + " i32";
                    Command command = new Command(0, false, command1) {
                        @Override
                        public void commandOutput(int id, String line) {
                            super.commandOutput(id, line);
                            String splits[] = line.split(" ");
                            try {
                                networkType = Integer.parseInt(splits[2]);
                            } catch (Exception e) {
                                networkType = -1;
                            }
                        }

                        @Override
                        public void commandTerminated(int id, String reason) {
                            super.commandTerminated(id, reason);
                        }

                        @Override
                        public void commandCompleted(int id, int exitCode) {
                            super.commandCompleted(id, exitCode);
                        }
                    };
                    try {
                        RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                        commandWait(command);
                    } catch (Exception e) {
                        Log.e("ActivateProfileHelper.setPreferredNetworkType", Log.getStackTraceString(e));
                    }
                }

            } catch(Exception e) {
                Log.e("ActivateProfileHelper.getPreferredNetworkType", Log.getStackTraceString(e));
            }
        }
        else
            networkType = -1;
        return networkType;
    }
    */

    static boolean telephonyServiceExists(/*Context context, */
            @SuppressWarnings("SameParameterValue") String preference) {
        try {
            Object serviceManager = PPApplication.getServiceManager("phone");
            if (serviceManager != null) {
                int transactionCode = -1;
                if (preference.equals(Profile.PREF_PROFILE_DEVICE_MOBILE_DATA))
                    transactionCode = PPApplication.getTransactionCode(String.valueOf(serviceManager), "setDataEnabled");
                else
                if (preference.equals(Profile.PREF_PROFILE_DEVICE_NETWORK_TYPE))
                    transactionCode = PPApplication.getTransactionCode(String.valueOf(serviceManager), "setPreferredNetworkType");
                return transactionCode != -1;
            }
            return false;
        } catch(Exception e) {
            return false;
        }
    }

    private static void setPreferredNetworkType(Context context, int networkType)
    {
        if (PPApplication.isRooted() && PPApplication.serviceBinaryExists())
        {
            try {
                // Get the value of the "TRANSACTION_setPreferredNetworkType" field.
                Object serviceManager = PPApplication.getServiceManager("phone");
                int transactionCode = -1;
                if (serviceManager != null) {
                    transactionCode = PPApplication.getTransactionCode(String.valueOf(serviceManager), "setPreferredNetworkType");
                }

                if (transactionCode != -1) {
                    // Android 6?
                    if (Build.VERSION.SDK_INT >= 23) {
                        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(context);
                        // Loop through the subscription list i.e. SIM list.
                        List<SubscriptionInfo> subscriptionList = mSubscriptionManager.getActiveSubscriptionInfoList();
                        if (subscriptionList != null) {
                            for (int i = 0; i < mSubscriptionManager.getActiveSubscriptionInfoCountMax(); i++) {
                                // Get the active subscription ID for a given SIM card.
                                SubscriptionInfo subscriptionInfo = subscriptionList.get(i);
                                if (subscriptionInfo != null) {
                                    int subscriptionId = subscriptionInfo.getSubscriptionId();
                                    synchronized (PPApplication.rootMutex) {
                                        String command1 = PPApplication.getServiceCommand("phone", transactionCode, subscriptionId, networkType);
                                        if (command1 != null) {
                                            Command command = new Command(0, false, command1);
                                            try {
                                                RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                                PPApplication.commandWait(command);
                                            } catch (Exception e) {
                                                Log.e("ActivateProfileHelper.setPreferredNetworkType", Log.getStackTraceString(e));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        synchronized (PPApplication.rootMutex) {
                            String command1 = PPApplication.getServiceCommand("phone", transactionCode, networkType);
                            if (command1 != null) {
                                Command command = new Command(0, false, command1);
                                try {
                                    RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                    PPApplication.commandWait(command);
                                } catch (Exception e) {
                                    Log.e("ActivateProfileHelper.setPreferredNetworkType", Log.getStackTraceString(e));
                                }
                            }
                        }
                    }
                }
            } catch(Exception ignored) {
            }
        }
    }

    static boolean wifiServiceExists(/*Context context, */
            @SuppressWarnings("SameParameterValue") String preference) {
        try {
            Object serviceManager = PPApplication.getServiceManager("wifi");
            if (serviceManager != null) {
                int transactionCode = -1;
                if (preference.equals(Profile.PREF_PROFILE_DEVICE_WIFI_AP))
                    transactionCode = PPApplication.getTransactionCode(String.valueOf(serviceManager), "setWifiApEnabled");
                return transactionCode != -1;
            }
            return false;
        } catch(Exception e) {
            return false;
        }
    }

    private static void setWifiAP(WifiApManager wifiApManager, boolean enable, Context context) {
        PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-enable="+enable);

        if (Build.VERSION.SDK_INT < 26) {
            PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-API < 26");
            wifiApManager.setWifiApState(enable);
        }
        else {
            PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-API >= 26");
            if (PPApplication.isRooted() && PPApplication.serviceBinaryExists()) {
                PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-rooted");
                try {
                    Object serviceManager = PPApplication.getServiceManager("wifi");
                    int transactionCode = -1;
                    if (serviceManager != null) {
                        transactionCode = PPApplication.getTransactionCode(String.valueOf(serviceManager), "setWifiApEnabled");
                    }
                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-serviceManager="+String.valueOf(serviceManager));
                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-transactionCode="+transactionCode);

                    if (transactionCode != -1) {
                        if (enable) {
                            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-wifiManager="+wifiManager);
                            if (wifiManager != null) {
                                int wifiState = wifiManager.getWifiState();
                                boolean isWifiEnabled = ((wifiState == WifiManager.WIFI_STATE_ENABLED) || (wifiState == WifiManager.WIFI_STATE_ENABLING));
                                PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-isWifiEnabled="+isWifiEnabled);
                                if (isWifiEnabled) {
                                    wifiManager.setWifiEnabled(false);
                                    PPApplication.sleep(1000);
                                }
                            }
                        }
                        synchronized (PPApplication.rootMutex) {
                            PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-start root command");
                            String command1 = PPApplication.getServiceCommand("wifi", transactionCode, 0, (enable) ? 1 : 0);
                            if (command1 != null) {
                                PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-command1=" + command1);
                                Command command = new Command(0, false, command1);
                                try {
                                    RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                    PPApplication.commandWait(command);
                                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-root command end");
                                } catch (Exception e) {
                                    Log.e("ActivateProfileHelper.setWifiAP", Log.getStackTraceString(e));
                                    PPApplication.logE("$$$ WifiAP", "ActivateProfileHelper.setWifiAP-root command error");
                                }
                            }
                        }
                    }
                } catch(Exception e) {
                    Log.e("ActivateProfileHelper.setWifiAP", Log.getStackTraceString(e));
                    PPApplication.logE("$$$ WifiAP", Log.getStackTraceString(e));
                }
            }
            else {
                if (enable)
                    wifiApManager.startTethering();
                else
                    wifiApManager.stopTethering();
            }
        }
    }

    private static void setNFC(Context context, boolean enable)
    {
        /*
        Not working in debug version of application !!!!
        Test with release version.
        */

        if (Permissions.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)) {
            CmdNfc.run(enable);
        }
        else
        if (PPApplication.isRooted()/*PPApplication.isRootGranted()*/) {
            synchronized (PPApplication.rootMutex) {
                String command1 = PPApplication.getJavaCommandFile(CmdNfc.class, "nfc", context, enable);
                if (command1 != null) {
                    Command command = new Command(0, false, command1);
                    try {
                        RootTools.getShell(true, Shell.ShellContext.NORMAL).add(command);
                        PPApplication.commandWait(command);
                    } catch (Exception e) {
                        Log.e("ActivateProfileHelper.setNFC", Log.getStackTraceString(e));
                    }
                }
                //String command = PPApplication.getJavaCommandFile(CmdNfc.class, "nfc", context, enable);
                //if (command != null)
                //  RootToolsSmall.runSuCommand(command);
            }
        }
    }

    static boolean canExploitGPS(Context context)
    {
        // test exploiting power manager widget
        PackageManager pacMan = context.getPackageManager();
        try {
            PackageInfo pacInfo = pacMan.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);

            if(pacInfo != null){
                for(ActivityInfo actInfo : pacInfo.receivers){
                    //test if receiver is exported. if so, we can toggle GPS.
                    if(actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported){
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false; //package not found
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static void setGPS(Context context, boolean enable)
    {
        //boolean isEnabled;
        //int locationMode = -1;
        //if (android.os.Build.VERSION.SDK_INT < 19)
        //    isEnabled = Settings.Secure.isLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER);
        /*else {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, -1);
            isEnabled = (locationMode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) ||
                        (locationMode == Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
        }*/

        boolean isEnabled = false;
        boolean ok = true;
        /*if (android.os.Build.VERSION.SDK_INT < 19)
            isEnabled = Settings.Secure.isLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER);
        else {*/
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null)
                isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            else
                ok = false;
        //}
        if (!ok)
            return;

        PPApplication.logE("ActivateProfileHelper.setGPS", "isEnabled="+isEnabled);

        //if(!provider.contains(LocationManager.GPS_PROVIDER) && enable)
        if ((!isEnabled) && enable)
        {
            // adb shell pm grant sk.henrichg.phoneprofilesplus android.permission.WRITE_SECURE_SETTINGS
            if (Permissions.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)) {
                String newSet;
                if (android.os.Build.VERSION.SDK_INT < 23) {
                    String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                    if (provider.equals(""))
                        newSet = LocationManager.GPS_PROVIDER;
                    else
                        newSet = String.format("%s,%s", provider, LocationManager.GPS_PROVIDER);
                }
                else
                    newSet = "+gps";
                Settings.Secure.putString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, newSet);
            }
            else
            if (PPApplication.isRooted() && PPApplication.settingsBinaryExists())
            {
                // device is rooted
                PPApplication.logE("ActivateProfileHelper.setGPS", "rooted");

                String command1;
                //String command2;

                if (android.os.Build.VERSION.SDK_INT < 23) {
                    String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                    PPApplication.logE("ActivateProfileHelper.setGPS", "provider="+provider);

                    String newSet;
                    if (provider.isEmpty())
                        newSet = LocationManager.GPS_PROVIDER;
                    else
                        newSet = String.format("%s,%s", provider, LocationManager.GPS_PROVIDER);
                    PPApplication.logE("ActivateProfileHelper.setGPS", "newSet="+newSet);

                    synchronized (PPApplication.rootMutex) {
                        command1 = "settings put secure location_providers_allowed \"" + newSet + "\"";
                        //if (PPApplication.isSELinuxEnforcing())
                        //	command1 = PPApplication.getSELinuxEnforceCommand(command1, Shell.ShellContext.SYSTEM_APP);

                        //command2 = "am broadcast -a android.location.GPS_ENABLED_CHANGE --ez state true";
                        Command command = new Command(0, false, command1); //, command2);
                        try {
                            RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                            PPApplication.commandWait(command);
                        } catch (Exception e) {
                            Log.e("ActivateProfileHelper.setGPS", Log.getStackTraceString(e));
                            PPApplication.logE("ActivateProfileHelper.setGPS", "Error on run su: " + e.toString());
                        }
                    }
                }
                else {
                    /*
                    String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                    PPApplication.logE("ActivateProfileHelper.setGPS", "provider="+provider);
                    */
                    synchronized (PPApplication.rootMutex) {
                        command1 = "settings put secure location_providers_allowed +gps";
                        Command command = new Command(0, false, command1);
                        try {
                            RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                            PPApplication.commandWait(command);
                        } catch (Exception e) {
                            PPApplication.logE("ActivateProfileHelper.setGPS", "Error on run su: " + e.toString());
                        }
                    }
                }
            }
            else
            if (canExploitGPS(context))
            {
                PPApplication.logE("ActivateProfileHelper.setGPS", "exploit");

                final Intent poke = new Intent();
                poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                context.sendBroadcast(poke);
            }
            //else
            //{
                /*PPApplication.logE("ActivateProfileHelper.setGPS", "old method");

                try {
                    Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
                    intent.putExtra("enabled", enable);
                    context.sendBroadcast(intent);
                } catch (SecurityException e) {
                    Log.e("ActivateProfileHelper.setGPS", Log.getStackTraceString(e));
                }*/

                // for normal apps it is only possible to open the system settings dialog
            /*	Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent); */
            //}
        }
        else
        //if(provider.contains(LocationManager.GPS_PROVIDER) && (!enable))
        if (isEnabled && (!enable))
        {
            // adb shell pm grant sk.henrichg.phoneprofilesplus android.permission.WRITE_SECURE_SETTINGS
            if (Permissions.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)) {
                String newSet = "";
                if (android.os.Build.VERSION.SDK_INT < 23) {
                    String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                    String[] list = provider.split(",");
                    int j = 0;
                    for (String aList : list) {
                        if (!aList.equals(LocationManager.GPS_PROVIDER)) {
                            if (j > 0)
                                //noinspection StringConcatenationInLoop
                                newSet += ",";
                            //noinspection StringConcatenationInLoop
                            newSet += aList;
                            j++;
                        }
                    }
                }
                else
                    newSet = "-gps";
                Settings.Secure.putString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, newSet);
            }
            else
            if (PPApplication.isRooted() && PPApplication.settingsBinaryExists())
            {
                // device is rooted
                PPApplication.logE("ActivateProfileHelper.setGPS", "rooted");

                String command1;
                //String command2;

                if (android.os.Build.VERSION.SDK_INT < 23) {
                    String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                    PPApplication.logE("ActivateProfileHelper.setGPS", "provider="+provider);

                    String[] list = provider.split(",");

                    String newSet = "";
                    int j = 0;
                    for (String aList : list) {

                        if (!aList.equals(LocationManager.GPS_PROVIDER)) {
                            if (j > 0)
                                //noinspection StringConcatenationInLoop
                                newSet += ",";
                            //noinspection StringConcatenationInLoop
                            newSet += aList;
                            j++;
                        }
                    }
                    PPApplication.logE("ActivateProfileHelper.setGPS", "newSet="+newSet);

                    synchronized (PPApplication.rootMutex) {
                        command1 = "settings put secure location_providers_allowed \"" + newSet + "\"";
                        //if (PPApplication.isSELinuxEnforcing())
                        //	command1 = PPApplication.getSELinuxEnforceCommand(command1, Shell.ShellContext.SYSTEM_APP);
                        //command2 = "am broadcast -a android.location.GPS_ENABLED_CHANGE --ez state false";
                        Command command = new Command(0, false, command1);//, command2);
                        try {
                            RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                            PPApplication.commandWait(command);
                        } catch (Exception e) {
                            Log.e("ActivateProfileHelper.setGPS", Log.getStackTraceString(e));
                            PPApplication.logE("ActivateProfileHelper.setGPS", "Error on run su: " + e.toString());
                        }
                    }
                }
                else {
                    /*
                    String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                    PPApplication.logE("ActivateProfileHelper.setGPS", "provider="+provider);
                    */
                    synchronized (PPApplication.rootMutex) {
                        command1 = "settings put secure location_providers_allowed -gps";
                        Command command = new Command(0, false, command1);
                        try {
                            RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                            PPApplication.commandWait(command);
                        } catch (Exception e) {
                            PPApplication.logE("ActivateProfileHelper.setGPS", "Error on run su: " + e.toString());
                        }
                    }
                }
            }
            else
            if (canExploitGPS(context))
            {
                PPApplication.logE("ActivateProfileHelper.setGPS", "exploit");

                final Intent poke = new Intent();
                poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                context.sendBroadcast(poke);
            }
            //else
            //{
                //PPApplication.logE("ActivateProfileHelper.setGPS", "old method");

                /*try {
                    Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
                    intent.putExtra("enabled", enable);
                    context.sendBroadcast(intent);
                } catch (SecurityException e) {
                    Log.e("ActivateProfileHelper.setGPS", Log.getStackTraceString(e));
                }*/

                // for normal apps it is only possible to open the system settings dialog
            /*	Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent); */
            //}
        }	    	
    }

    private static void setAirplaneMode_SDK17(/*Context context, */boolean mode)
    {
        if (PPApplication.isRooted() && PPApplication.settingsBinaryExists())
        {
            // device is rooted
            synchronized (PPApplication.rootMutex) {
                String command1;
                String command2;
                if (mode) {
                    command1 = "settings put global airplane_mode_on 1";
                    command2 = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true";
                } else {
                    command1 = "settings put global airplane_mode_on 0";
                    command2 = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false";
                }
                //if (PPApplication.isSELinuxEnforcing())
                //{
                //	command1 = PPApplication.getSELinuxEnforceCommand(command1, Shell.ShellContext.SYSTEM_APP);
                //	command2 = PPApplication.getSELinuxEnforceCommand(command2, Shell.ShellContext.SYSTEM_APP);
                //}
                Command command = new Command(0, true, command1, command2);
                try {
                    RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                    PPApplication.commandWait(command);
                } catch (Exception e) {
                    Log.e("AirPlaneMode_SDK17.setAirplaneMode", Log.getStackTraceString(e));
                }
                PPApplication.logE("ActivateProfileHelper.setAirplaneMode_SDK17", "done");
            }
        }
        //else
        //{
            // for normal apps it is only possible to open the system settings dialog
        /*	Intent intent = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent); */
        //}
    }

    /*
    private void setAirplaneMode_SDK8(Context context, boolean mode)
    {
        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, mode ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", mode);
        context.sendBroadcast(intent);
    }
    */

    private static void setPowerSaveMode(final Profile profile, final Context context) {
        if (profile._devicePowerSaveMode != 0) {
            final Context appContext = context.getApplicationContext();
            PPApplication.startHandlerThreadPowerSaveMode();
            final Handler handler = new Handler(PPApplication.handlerThreadPowerSaveMode.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_POWER_SAVE_MODE, appContext).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {

                        PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                        PowerManager.WakeLock wakeLock = null;
                        if (powerManager != null) {
                            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivateProfileHelper.setPowerSaveMode");
                            wakeLock.acquire(10 * 60 * 1000);
                        }

                        if (powerManager != null) {
                            //PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                            boolean _isPowerSaveMode = false;
                            if (Build.VERSION.SDK_INT >= 21)
                                _isPowerSaveMode = powerManager.isPowerSaveMode();
                            boolean _setPowerSaveMode = false;
                            switch (profile._devicePowerSaveMode) {
                                case 1:
                                    if (!_isPowerSaveMode) {
                                        _isPowerSaveMode = true;
                                        _setPowerSaveMode = true;
                                    }
                                    break;
                                case 2:
                                    if (_isPowerSaveMode) {
                                        _isPowerSaveMode = false;
                                        _setPowerSaveMode = true;
                                    }
                                    break;
                                case 3:
                                    _isPowerSaveMode = !_isPowerSaveMode;
                                    _setPowerSaveMode = true;
                                    break;
                            }
                            if (_setPowerSaveMode) {
                                if (Permissions.hasPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS)) {
                                    if (android.os.Build.VERSION.SDK_INT >= 21)
                                        Settings.Global.putInt(context.getContentResolver(), "low_power", ((_isPowerSaveMode) ? 1 : 0));
                                } else if (PPApplication.isRooted() && PPApplication.settingsBinaryExists()) {
                                    synchronized (PPApplication.rootMutex) {
                                        String command1 = "settings put global low_power " + ((_isPowerSaveMode) ? 1 : 0);
                                        Command command = new Command(0, false, command1);
                                        try {
                                            RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                            PPApplication.commandWait(command);
                                        } catch (Exception e) {
                                            Log.e("ActivateProfileHelper.setPowerSaveMode", Log.getStackTraceString(e));
                                        }
                                    }
                                }
                            }
                        }

                        if ((wakeLock != null) && wakeLock.isHeld()) {
                            try {
                                wakeLock.release();
                            } catch (Exception ignored) {}
                        }
                    }
                }
            });
        }
    }

    private static void lockDevice(final Profile profile, final Context context) {
        final Context appContext = context.getApplicationContext();
        PPApplication.startHandlerThreadLockDevice();
        final Handler handler = new Handler(PPApplication.handlerThreadLockDevice.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (PPApplication.startedOnBoot)
                    // not lock device after boot
                    return;

                PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivateProfileHelper.lockDevice");
                    wakeLock.acquire(10 * 60 * 1000);
                }

                switch (profile._lockDevice) {
                    case 2:
                        if (PPApplication.isRooted())
                        {
                            synchronized (PPApplication.rootMutex) {
                                /*String command1 = "input keyevent 26";
                                Command command = new Command(0, false, command1);
                                try {
                                    RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                    commandWait(command);
                                } catch (Exception e) {
                                    Log.e("ActivateProfileHelper.lockDevice", Log.getStackTraceString(e));
                                }*/
                                String command1 = PPApplication.getJavaCommandFile(CmdGoToSleep.class, "power", context, 0);
                                if (command1 != null) {
                                    Command command = new Command(0, false, command1);
                                    try {
                                        RootTools.getShell(true, Shell.ShellContext.NORMAL).add(command);
                                        PPApplication.commandWait(command);
                                    } catch (Exception e) {
                                        Log.e("ActivateProfileHelper.lockDevice", Log.getStackTraceString(e));
                                    }
                                }
                            }
                        }
                        /*if (PPApplication.isRooted() && PPApplication.serviceBinaryExists()) {
                            synchronized (PPApplication.rootMutex) {
                                try {
                                    // Get the value of the "TRANSACTION_goToSleep" field.
                                    String transactionCode = PPApplication.getTransactionCode("android.os.IPowerManager", "TRANSACTION_goToSleep");
                                    String command1 = "service call power " + transactionCode + " i64 " + SystemClock.uptimeMillis();
                                    Command command = new Command(0, false, command1);
                                    try {
                                        RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP).add(command);
                                        commandWait(command);
                                    } catch (Exception e) {
                                        Log.e("ActivateProfileHelper.lockDevice", Log.getStackTraceString(e));
                                    }
                                } catch(Exception ignored) {
                                }
                            }
                        */
                        break;
                    case 1:
                    case 3:
                        if (PhoneProfilesService.getInstance() != null) {
                            if (Permissions.checkLockDevice(context) && (PhoneProfilesService.getInstance().lockDeviceActivity == null)) {
                                try {
                                    Intent intent = new Intent(context, LockDeviceActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    context.startActivity(intent);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                        break;
                }

                if ((wakeLock != null) && wakeLock.isHeld()) {
                    try {
                        wakeLock.release();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    /*
    private static void setScreenNightMode(Context context, final int value) {
        if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_SCREEN_NIGHT_MODE, context)
                == PreferenceAllowed.PREFERENCE_ALLOWED) {
            UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
            PPApplication.logE("ActivateProfileHelper.setScreenNightMode", "uiModeManager="+uiModeManager);
            if (uiModeManager != null) {
                switch (value) {
                    case 1:
                        uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
                        break;
                    case 2:
                        uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
                        break;
                    case 3:
                        uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);
                        break;
                }
            }
        }
    }
    */

    static int getRingerVolume(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getInt(PREF_RINGER_VOLUME, -999);
    }

    static void setRingerVolume(Context context, int volume)
    {
        PPApplication.logE("ActivateProfileHelper.(s)setRingerVolume","volume="+volume);
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(PREF_RINGER_VOLUME, volume);
        editor.apply();
    }

    private static int getNotificationVolume(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getInt(PREF_NOTIFICATION_VOLUME, -999);
    }

    static void setNotificationVolume(Context context, int volume)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(PREF_NOTIFICATION_VOLUME, volume);
        editor.apply();
    }

    static int getRingerMode(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getInt(PREF_RINGER_MODE, 0);
    }

    static void setRingerMode(Context context, int mode)
    {
        PPApplication.logE("ActivateProfileHelper.(s)setRingerMode","mode="+mode);
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(PREF_RINGER_MODE, mode);
        editor.apply();
    }

    static int getZenMode(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getInt(PREF_ZEN_MODE, 0);
    }

    static void setZenMode(Context context, int mode)
    {
        PPApplication.logE("ActivateProfileHelper.(s)setZenMode","mode="+mode);
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(PREF_ZEN_MODE, mode);
        editor.apply();
    }

    static boolean getLockScreenDisabled(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getBoolean(PREF_LOCKSCREEN_DISABLED, false);
    }

    static void setLockScreenDisabled(Context context, boolean disabled)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putBoolean(PREF_LOCKSCREEN_DISABLED, disabled);
        editor.apply();
    }

    /*
    private static boolean getScreenUnlocked(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getBoolean(PREF_SCREEN_UNLOCKED, true);
    }

    static void setScreenUnlocked(Context context, boolean unlocked)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putBoolean(PREF_SCREEN_UNLOCKED, unlocked);
        editor.apply();
    }
    */

    static int getActivatedProfileScreenTimeout(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getInt(PREF_ACTIVATED_PROFILE_SCREEN_TIMEOUT, 0);
    }

    static void setActivatedProfileScreenTimeout(Context context, int timeout)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(PREF_ACTIVATED_PROFILE_SCREEN_TIMEOUT, timeout);
        editor.apply();
    }

}
