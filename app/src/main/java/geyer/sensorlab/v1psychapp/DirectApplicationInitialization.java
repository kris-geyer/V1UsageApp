package geyer.sensorlab.v1psychapp;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
class DirectApplicationInitialization {

    private static final String TAG = "DAI";

    private Boolean informUserRequired,
            passwordRequired,
            requestUsagePermission,
            requestNotificationPermission,
            performCrossSectionalAnalysis,
            prospectiveLoggingEmployed,
            retrospectiveLoggingEmployed;


    private int levelOfCrossSectionalAnalysis,
            levelOfProspectiveLogging,
            levelOfRetrospectiveAnalysis;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private AppOpsManager appOpsManager;
    private ActivityManager manager;

    private String pkg;
    private MainActivity mainActivityContext;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    DirectApplicationInitialization(Boolean InformUserRequired, Boolean PasswordRequired, Boolean RequestUsagePermission, Boolean RequestNotificationPermission,
                                    Boolean PerformCrossSectionalAnalysis, int LevelOfCrossSectionalAnalysis, Boolean ProspectiveLoggingEmployed, int LevelOfProspectiveLogging,
                                    Boolean RetrospectiveLoggingEmployed, int LevelOfRetrospectiveAnalysis,
                                    SharedPreferences sharedPreferencesFromMain, Object systemService1, Object o, Object systemService, MainActivity mainActivity) {
        informUserRequired = InformUserRequired;
        passwordRequired = PasswordRequired;
        requestUsagePermission = RequestUsagePermission;
        requestNotificationPermission = RequestNotificationPermission;
        performCrossSectionalAnalysis = PerformCrossSectionalAnalysis;
        levelOfCrossSectionalAnalysis = LevelOfCrossSectionalAnalysis;
        prospectiveLoggingEmployed = ProspectiveLoggingEmployed;
        levelOfProspectiveLogging = LevelOfProspectiveLogging;
        retrospectiveLoggingEmployed = RetrospectiveLoggingEmployed;
        levelOfRetrospectiveAnalysis = LevelOfRetrospectiveAnalysis;

        sharedPreferences = sharedPreferencesFromMain;
        editor = sharedPreferences.edit();
        editor.apply();

        appOpsManager= (AppOpsManager) systemService1;
        manager = (ActivityManager) o;
        pkg = (String) systemService;
        mainActivityContext = mainActivity;

    }

    /**
     * States:
     * 1 - inform user
     * 2 - request password
     * 3 - document apps & permissions
     * 4 - request usage permission
     * 5 - request notification permission
     * 6 - All permission provided
     * 7 - start Service
     * 8 - start NotificationListenerService
     * 9 - retrospectively log data
     * 10 - retrospective data generating complete
     * 11 - service running
     */

     int detectState(){
        int state = 1;

        if(sharedPreferences.getBoolean("instructions shown", false) || !informUserRequired){
            state = 2;
            if(sharedPreferences.getBoolean("password generated", false) || !passwordRequired){

                    if(fileExists(Constants.APPS_AND_PERMISSIONS_FILE) || !performCrossSectionalAnalysis){
                        state = detectPermissionsState();
                        if(state == 6){
                            if(prospectiveLoggingEmployed){
                                if(requestNotificationPermission){
                                    state = 8;
                                    if(serviceIsRunning(notificationLogger.class))
                                        state = 11;
                                }else{
                                    state = 7;
                                    if(serviceIsRunning(logger.class))
                                        state = 11;
                                }
                            }

                            if(retrospectiveLoggingEmployed){
                                if(fileExists(Constants.PAST_USAGE_FILE)) {
                                    state = 10;
                                }else{
                                    state = 9;
                                }
                            }
                        }

                    }else{
                        state = 3;
                    }
            }
        }
        return state;
    }

    private boolean fileExists(String file) {
        String directory = (String.valueOf(mainActivityContext.getFilesDir()) + File.separator);
        File inspectedFile = new File(directory + File.separator + file);
        return inspectedFile.exists();
    }

    //This establishes what permission is required based on what the researchers have indicated that they wish to include in their study.
    private int detectPermissionsState() {

        Boolean permissionsStillRequired = requestUsagePermission || requestNotificationPermission;

        if(permissionsStillRequired){
            if(!requestNotificationPermission){
                if(establishStateOfUsageStatisticsPermission()){
                    Log.i(TAG, "usage statistics permission granted");
                    return 6;
                }else{
                    return 4;
                }
            }else if(!requestUsagePermission){
                if(establishStateOfNotificationListenerPermission()){
                    Log.i(TAG, "notification listener permission granted");
                    return 6;
                }else{
                    return 5;
                }
            }else{
                if(establishStateOfUsageStatisticsPermission()){
                    if(establishStateOfNotificationListenerPermission()){
                        Log.i(TAG, "all permissions permission granted");
                        return 6;
                    }else{
                        return 5;
                    }
                }else{
                    return 4;
                }
            }
        }else{
            return 6;
        }

    }


    //establishes if the usage statistics permissions are provided
    private Boolean establishStateOfUsageStatisticsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int mode = 2;
            if (appOpsManager != null) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), String.valueOf(pkg));
            }
            return (mode == AppOpsManager.MODE_ALLOWED);
        }else{
            return true;
        }
    }

    private boolean establishStateOfNotificationListenerPermission() {
        ComponentName cn = new ComponentName(mainActivityContext, notificationLogger.class);
        String flat = Settings.Secure.getString(mainActivityContext.getContentResolver(), "enabled_notification_listeners");
        return flat == null || flat.contains(cn.flattenToString());
    }

    //detects if the background logging behaviour is running, this will not detect if data is being collected.
    private boolean serviceIsRunning(Class<?> serviceClass) {
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void directState(int state) {

    }
}
