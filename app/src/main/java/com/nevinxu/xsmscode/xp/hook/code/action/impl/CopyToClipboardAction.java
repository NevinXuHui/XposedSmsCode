package com.nevinxu.xsmscode.xp.hook.code.action.impl;

import android.content.Context;
import android.os.Bundle;

import com.nevinxu.xsmscode.common.utils.ClipboardUtils;
import com.nevinxu.xsmscode.common.utils.XSPUtils;
import com.nevinxu.xsmscode.data.db.entity.SmsMsg;
import com.nevinxu.xsmscode.xp.hook.code.action.RunnableAction;

import de.robv.android.xposed.XSharedPreferences;

/**
 * 将验证码复制到剪切板
 */
public class CopyToClipboardAction extends RunnableAction {

    public CopyToClipboardAction(Context appContext, Context phoneContext, SmsMsg smsMsg, XSharedPreferences xsp) {
        super(appContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        if (XSPUtils.copyToClipboardEnabled(xsp)) {
            copyToClipboard();
        }
        return null;
    }

    private void copyToClipboard() {
        ClipboardUtils.copyToClipboard(mAppContext, mSmsMsg.getSmsCode());
    }
}
