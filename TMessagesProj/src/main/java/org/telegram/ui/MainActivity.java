/*
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Dev Concert, 2014.
 */

package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.LinearLayout;

import org.telegram.messenger.BuildVars;
import me.ttalk.sdk.ServiceAgent;
import me.ttalk.sdk.theme.ThemeManager;
import org.telegram.messenger.R;

public class MainActivity extends Activity {

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
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        int screenSizeType = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
        if(screenSizeType == Configuration.SCREENLAYOUT_SIZE_XLARGE || screenSizeType == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.main_layout_tablet);
        }
        else if(screenSizeType == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.main_layout);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.main_layout);
        }

        LinearLayout ll_intro = (LinearLayout) findViewById(R.id.ll_intro);
        Drawable drawable = ThemeManager.getInstance().getRemoteResourceDrawable("background_intro");
        if (drawable != null) {
            if (Build.VERSION.SDK_INT >= 16) {
                ll_intro.setBackground(drawable);
            } else {
                ll_intro.setBackgroundDrawable(drawable);
            }
        }

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
//                ServiceAgent.getInstance().logEvent("Main", "LaunchActivity");
                Intent i = new Intent(MainActivity.this, LaunchActivity.class);
                startActivity(i);
                finish();
            }
        }, 1000);
    }
}
