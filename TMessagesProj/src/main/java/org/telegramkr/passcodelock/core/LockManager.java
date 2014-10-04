package org.telegramkr.passcodelock.core;

import android.app.Application;

public class LockManager {

    private volatile static LockManager instance;
    private PasscodeLock curAppLocker;

    public static LockManager getInstance() {
        synchronized (LockManager.class) {
            if (instance == null) {
                instance = new LockManager();
            }
        }
        return instance;
    }

    public void enableAppLock(Application app) {
        if (curAppLocker == null) {
            curAppLocker = new PasscodeLockImpl(app);
        }
        curAppLocker.enable();
    }

    public boolean isAppLockEnabled() {
        if (curAppLocker == null) {
            return false;
        } else {
            return true;
        }
    }

    public void setAppLock(PasscodeLock appLocker) {
        if (curAppLocker != null) {
            curAppLocker.disable();
        }
        curAppLocker = appLocker;
    }

    public PasscodeLock getAppLock() {
        return curAppLocker;
    }
}
