package geyer.sensorlab.v1psychapp;

import android.content.Intent;
import android.provider.Settings;

class PermissionRequestsAndCrossSectionalAnalysis {

    private MainActivity activityContext;
    private static final int REQUEST_USAGE_STATISTICS = 101;

    PermissionRequestsAndCrossSectionalAnalysis(MainActivity mainActivity) {
        activityContext = mainActivity;
    }

    public void requestPermission(){
        activityContext.startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), REQUEST_USAGE_STATISTICS);
    }



}
