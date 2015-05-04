/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package org.telegram.android;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.ApplicationLoader;

import java.util.List;

public class NotificationsService extends Service {
    private final String LOG_NAME = NotificationsService.class.getSimpleName();

    public static Thread mThread;

    private ComponentName recentComponentName;
    private ActivityManager mActivityManager;

    private boolean serviceRunning = false;

    @Override
    public void onCreate() {
        FileLog.e("tmessages", "service started");

        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        serviceRunning = true;
        ApplicationLoader.postInitApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

// FIXME:
//        if (mThread == null) {
//            mThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    while (serviceRunning) {
//                        List<ActivityManager.RecentTaskInfo> info = mActivityManager.getRecentTasks(1, Intent.FLAG_ACTIVITY_NEW_TASK);
//                        if (info != null) {
//                            ActivityManager.RecentTaskInfo recent = info.get(0);
//                            Intent mIntent = recent.baseIntent;
//                            ComponentName name = mIntent.getComponent();
//                            if (name.equals(recentComponentName)) {
//                                FileLog.e("tmessages", LOG_NAME + ": present App == recent App");
//                            } else {
//                                recentComponentName = name;
//                                FileLog.e("tmessages", LOG_NAME + ": Cached" + name);
//                            }
//                        }
//                        SystemClock.sleep(2000);
//                    }
//                }
//            });
//
//            mThread.start();
//        } else if (mThread.isAlive() == false) {
//            mThread.start();
//        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        FileLog.e("tmessages", "service destroyed");
        serviceRunning = false;

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);
        if (preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("org.telegram.start");
            sendBroadcast(intent);
        }
    }
}
