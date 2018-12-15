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


    public StringBuilder buildMessageToInformUser() {
        StringBuilder toRelay = new StringBuilder();
        toRelay.append("This app is intended to relay information about how much you use this smartphone" +"\n").
                append("\n").append("This app functions by ");
        if(prospectiveLoggingEmployed && !retrospectiveLoggingEmployed){
            toRelay.append("documenting how you use your phone by running a program in the background. ");
        }else if (!prospectiveLoggingEmployed && retrospectiveLoggingEmployed){
            toRelay.append("querying a database stored by android phones about what you have used your phone for previously. ");
        }else if(prospectiveLoggingEmployed && retrospectiveLoggingEmployed){
            toRelay.append("querying a database stored by android phones about what you have used your phone for previously and documenting how you use your phone by running a program in the background. ");
        }
        if(prospectiveLoggingEmployed){
            if(requestUsagePermission && !requestNotificationPermission){
                toRelay.append("What you use your smartphone for will be documented. ");
            }else if(!requestUsagePermission && requestNotificationPermission){
                toRelay.append("What notifications appear and which app called them will be documented as well as when you deleted them. ");
            }else if(requestUsagePermission && requestNotificationPermission){
                toRelay.append("What you use your smartphone for will be documented and ehat notifications appear and which app called them will be documented as well as when you deleted them. ");
            }

        }

        if(performCrossSectionalAnalysis){
            toRelay.append("The app will also seek to generate context by identify the current state of your phone. ");
                    switch (levelOfCrossSectionalAnalysis){
                        case 1:
                            toRelay.append("What apps are installed on your phone will be documented. ");
                            break;
                        case 2:
                            toRelay.append("What permissions are requested by the apps on your phone will be documented and your if you have granted this permission on not. ");
                            break;
                        case 3:
                            toRelay.append("What apps are installed on your phone will be documented as well as what permissions are requested and if you've granted access to the permission or not. ");
                            break;
                    }
        }

            toRelay.append("The phone has adopted a number of security measures to protect your privacy and data. ").append("\n").append("\n");
        if(passwordRequired){
            toRelay.append("Your data will be stored on at least a password protected 128-bit encryption.");
        }
        toRelay.append("You can delete all the data that hasn't been relayed to a researcher by simply uninstalling the app. The data will only be relayed once you press email and send the email that is generated with the data attached.");

        return toRelay;
    }
}
