package com.nevinxu.xsmscode.xp.hook.code.action.impl;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;

import com.github.nevinxu0725.xposed.smscode.BuildConfig;
import com.nevinxu.xsmscode.common.utils.XLog;
import com.nevinxu.xsmscode.common.utils.XSPUtils;
import com.nevinxu.xsmscode.data.db.entity.SmsMsg;
import com.nevinxu.xsmscode.xp.hook.code.action.CallableAction;

import de.robv.android.xposed.XSharedPreferences;

public class KillMeAction extends CallableAction {

    public KillMeAction(Context appContext, Context phoneContext, SmsMsg smsMsg, XSharedPreferences xsp) {
        super(appContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        killMe();
        return null;
    }

    private void killMe() {
        if (XSPUtils.killMeEnabled(xsp)) {
            killBackgroundProcess(BuildConfig.APPLICATION_ID);
        }
    }

    /**
     * android.app.ActivityManager#killBackgroundProcess()
     */
    @SuppressLint("MissingPermission")
    private void killBackgroundProcess(String packageName) {
        try {
            ActivityManager activityManager = (ActivityManager) mAppContext.getSystemService(Context.ACTIVITY_SERVICE);

            if (activityManager != null) {
                activityManager.killBackgroundProcesses(packageName);
                XLog.d("Kill %s background process succeed", packageName);
            }
        } catch (Throwable e) {
            XLog.e("Error occurs when kill background process %s", packageName, e);
        }
    }
}
