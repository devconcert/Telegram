package org.telegramkr.messenger.sdk.theme;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.ui.MainActivity;

import me.ttalk.sdk.R;
import me.ttalk.sdk.ServiceAgent;
import me.ttalk.sdk.theme.ThemeManager;

public class ThemeApplyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setTheme(R.style.Theme_TMessages);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main_layout);

        if(getIntent()!=null){
            Uri uri = getIntent().getData();
            try {
                uriProcessing(uri);
            }catch(Exception e){

            }
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        ServiceAgent.getInstance(this, BuildVars.DEBUG_VERSION, BuildVars.SESSION_KEY).startSession(this);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        ServiceAgent.getInstance().endSession(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent!=null){
            Uri uri = intent.getData();
            try {
                uriProcessing(uri);
            }catch(Exception e){

            }
        }
    }

    private void uriProcessing(Uri uri){
        if(uri != null) {
            String packageName = "";
            String category = uri.getQueryParameter("category");
            boolean applyTheme = false;
            if(category.equals("intro")){
                applyTheme = true;
            }else if(category.equals("all")){
                applyTheme = true;
            }

            if(applyTheme){
                packageName = uri.getQueryParameter("package");
                if(packageName != null ) {
                    PackageManager packageManager = getPackageManager();
                    String telegramPermission = "org.telegramkr.theme.users";
                    if (PackageManager.PERMISSION_GRANTED == packageManager.checkPermission(telegramPermission, packageName)){
                        ApplicationLoader.applicationContext = this;
                        ServiceAgent.getInstance().logEvent("Theme", "Apply", packageName);
                    }
                }else{
                    Toast.makeText(this, ApplicationLoader.applicationContext.getString(R.string.theme_package_wrong), Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, ApplicationLoader.applicationContext.getString(R.string.theme_package_wrong), Toast.LENGTH_SHORT).show();
            }
            finish();
            ThemeManager.getInstance().restartApp(MainActivity.class);
        }
    }

}
