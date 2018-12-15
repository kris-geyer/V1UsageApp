package geyer.sensorlab.v1psychapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, asyncResponse {

    PermissionRequestsAndCrossSectionalAnalysis prANDcsa;
    DirectApplicationInitialization dai;
    documentApps docApps;

    BroadcastReceiver localListener;
    ProgressBar progressBar;

    SharedPreferences prefs;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeServiceStateListener();
        initializeComponents();
        initializeClasses();
        initializeButton();
        dai.directState(dai.detectState());
    }

    private void initializeComponents() {
        prefs = getSharedPreferences("app initialization prefs", MODE_PRIVATE);
    }

    private void initializeButton() {
        progressBar = findViewById(R.id.pb);
        Button request = findViewById(R.id.btnRequest);
        request.setOnClickListener(this);
        Button email = findViewById(R.id.btnEmail);
        email.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void initializeClasses() {
        dai = new DirectApplicationInitialization(
                false,
                false,
                false,
                false,
                true,
                1,
                false,
                2,
                false,
                0,
                prefs,
                getSystemService(Context.APP_OPS_SERVICE),
                getSystemService(Context.ACTIVITY_SERVICE),
                getPackageName(),
                this
        );
        prANDcsa = new PermissionRequestsAndCrossSectionalAnalysis(this);
        docApps = new documentApps(this);
    }

    //this sets up some architecture in the code that will listen for significant signals from the background logging
    //this will indicate that the data collection is functioning or there is a problem, in a way that the participant can be informed
    private void initializeServiceStateListener() {

        localListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra("dataFromService", false)){
                    Log.i("FROM service", "data collection on going");
                    //relay that service if functioning properly
                }
                if(intent.getBooleanExtra("errorFromService", false)){
                    final String msg = intent.getStringExtra("dataToRelay");
                    Log.i("FROM service", msg);
                    //change string value to msg
                }
                if(intent.getBooleanExtra("progress bar update", false)){
                    progressBar.setProgress(intent.getIntExtra("progress bar progress", 0));
                }
            }
        };
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(localListener, new IntentFilter("changeInService"));
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnRequest:
                promptAction(dai.detectState());
                break;
            case R.id.btnEmail:
                sendEmail();
                break;
        }
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("text/plain");

        //getting directory for internal files
        String directory = (String.valueOf(this.getFilesDir()) + File.separator);
        Log.i("Directory", directory);

        //initializing files reference
        File appDocumented = new File(directory + File.separator + Constants.APPS_AND_PERMISSIONS_FILE),

                pastUsage = new File(directory + File.separator + Constants.PAST_USAGE_FILE);

        //list of files to be uploaded
        ArrayList<Uri> files = new ArrayList<>();

        //if target files are identified to exist then they are packages into the attachments of the email
        try {
            if(appDocumented.exists()){
                files.add(FileProvider.getUriForFile(this, "geyer.sensorlab.v1psychapp.fileprovider", appDocumented));
            }

            if(pastUsage.exists()){
                files.add(FileProvider.getUriForFile(this, "geyer.sensorlab.v1psychapp.fileprovider", pastUsage));
            }

            if(files.size() >0){
                //adds the file to the intent to send multiple data points
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }else{
                Log.e("email", "no files to upload");
            }

        }
        catch (Exception e){
            Log.e("File upload error1", "Error:" + e);
        }
    }


    private void promptAction(int i) {
        switch (i){
            case 3:
                docApps.execute(getApplicationContext(), this);
                break;
            default:
                Log.i("result", String.valueOf(i));
                break;
        }
    }

    @Override
    public void processFinish(Boolean output) {
        if(output){
            Toast.makeText(this, "works", Toast.LENGTH_SHORT).show();
        }
    }
}
