package com.github.tianma8023.xposed.smscode.xp.hook;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Telephony;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.Toast;

import com.crossbowffs.remotepreferences.RemotePreferences;
import com.github.tianma8023.xposed.smscode.BuildConfig;
import com.github.tianma8023.xposed.smscode.R;
import com.github.tianma8023.xposed.smscode.aidl.SmsMsg;
import com.github.tianma8023.xposed.smscode.aidl.SmsMsgDao;
import com.github.tianma8023.xposed.smscode.constant.PrefConst;
import com.github.tianma8023.xposed.smscode.db.DBProvider;
import com.github.tianma8023.xposed.smscode.utils.ClipboardUtils;
import com.github.tianma8023.xposed.smscode.utils.RemotePreferencesUtils;
import com.github.tianma8023.xposed.smscode.utils.SPUtils;
import com.github.tianma8023.xposed.smscode.utils.SmsCodeUtils;
import com.github.tianma8023.xposed.smscode.utils.StringUtils;
import com.github.tianma8023.xposed.smscode.utils.XLog;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class SmsCodeWorker {

    private static final int OP_DELETE = 0;
    private static final int OP_MARK_AS_READ = 1;

    @IntDef({OP_DELETE, OP_MARK_AS_READ})
    @interface SmsOp {
    }

    private Context mAppContext;
    private RemotePreferences mPreferences;
    private Intent mSmsIntent;

    private static final int MSG_QUIT_QUEUE = 0xff;
    private static final int MSG_MARK_AS_READ = 0xfe;
    private static final int MSG_DELETE_SMS = 0xfd;
    private static final int MSG_SHOW_TOAST = 0xfc;
    private static final int MSG_RECORD_SMS_MSG = 0xfb;
    private static final int MSG_KILL_ME = 0xfa;
    private static final int MSG_AUTO_INPUT_CODE = 0xf9;

    private Handler uiHandler;
    private Handler workerHandler;

    public SmsCodeWorker(Context appContext, Intent smsIntent) {
        mAppContext = appContext;
        mPreferences = RemotePreferencesUtils.getDefaultRemotePreferences(mAppContext);
        mSmsIntent = smsIntent;

        HandlerThread workerThread = new HandlerThread("SmsCodeWorker");
        workerThread.start();
        workerHandler = new WorkerHandler(workerThread.getLooper());

        uiHandler = new WorkerHandler(Looper.getMainLooper());
    }

    public ParseResult parse() {
        if (!SPUtils.isEnabled(mPreferences)) {
            XLog.i("SmsCode disabled, exiting");
            return null;
        }

        SmsMsg smsMsg = SmsMsg.fromIntent(mSmsIntent);

        String sender = smsMsg.getSender();
        String msgBody = smsMsg.getBody();
        if (BuildConfig.DEBUG) {
            XLog.d("Sender: %s", sender);
            XLog.d("Body: %s", msgBody);
        } else {
            XLog.d("Sender: %s", StringUtils.escape(sender));
            XLog.d("Body: %s", StringUtils.escape(msgBody));
        }
        if (TextUtils.isEmpty(msgBody)) {
            return null;
        }

        String smsCode = SmsCodeUtils.parseSmsCodeIfExists(mAppContext, msgBody);
        smsMsg.setSmsCode(smsCode);
        if (TextUtils.isEmpty(smsCode)) { // Not verification code msg.
            return null;
        }

        boolean verboseLog = SPUtils.isVerboseLogMode(mPreferences);
        if (verboseLog) {
            XLog.setLogLevel(Log.VERBOSE);
        } else {
            XLog.setLogLevel(BuildConfig.LOG_LEVEL);
        }

        // 是否复制到剪切板
        if (SPUtils.copyToClipboardEnabled(mPreferences)) {
            ClipboardUtils.copyToClipboard(mAppContext, smsCode);
        }

        // 是否显示toast
        if (SPUtils.shouldShowToast(mPreferences)) {
            Message toastMsg = uiHandler.obtainMessage(MSG_SHOW_TOAST, smsCode);
            uiHandler.sendMessage(toastMsg);
        }

        // 是否自动输入
        if (SPUtils.autoInputCodeEnabled(mPreferences)) {
            Message autoInputMsg = workerHandler.obtainMessage(MSG_AUTO_INPUT_CODE, smsMsg);
            workerHandler.sendMessage(autoInputMsg);
        }

        // 是否记录验证码短信
        if (SPUtils.recordSmsCodeEnabled(mPreferences)) {
            smsMsg.setCompany(SmsCodeUtils.parseCompany(msgBody));
            smsMsg.setDate(System.currentTimeMillis());

            Message recordMsg = workerHandler.obtainMessage(MSG_RECORD_SMS_MSG, smsMsg);
            workerHandler.sendMessage(recordMsg);
        }

        // 是否删除验证码短信
        if (SPUtils.deleteSmsEnabled(mPreferences)) {
            Message deleteMsg = workerHandler.obtainMessage(MSG_DELETE_SMS, smsMsg);
            workerHandler.sendMessageDelayed(deleteMsg, 3000);
        } else {
            // 是否标记验证码短信为已读
            if (SPUtils.markAsReadEnabled(mPreferences)) {
                Message markMsg = workerHandler.obtainMessage(MSG_MARK_AS_READ, smsMsg);
                workerHandler.sendMessageDelayed(markMsg, 3000);
            }
        }

        // 是否自杀
        if (SPUtils.killMeEnabled(mPreferences)) {
            workerHandler.sendEmptyMessageDelayed(MSG_KILL_ME, 4000);
        }

        // 结束Looper
        workerHandler.sendEmptyMessageDelayed(MSG_QUIT_QUEUE, 5000);

        ParseResult parseResult = new ParseResult();
        parseResult.setBlockSms(SPUtils.blockSmsEnabled(mPreferences));
        return parseResult;
    }

    private class WorkerHandler extends Handler {

        WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_TOAST: {
                    showToast((String) msg.obj);
                    break;
                }
                case MSG_DELETE_SMS: {
                    SmsMsg smsMsg = (SmsMsg) msg.obj;
                    deleteSms(smsMsg.getSender(), smsMsg.getBody());
                    break;
                }
                case MSG_MARK_AS_READ: {
                    SmsMsg smsMsg = (SmsMsg) msg.obj;
                    markSmsAsRead(smsMsg.getSender(), smsMsg.getBody());
                    break;
                }
                case MSG_RECORD_SMS_MSG: {
                    SmsMsg smsMsg = (SmsMsg) msg.obj;
                    recordSmsMsg(smsMsg);
                    break;
                }
                case MSG_AUTO_INPUT_CODE: {
                    SmsMsg smsMsg = (SmsMsg) msg.obj;
                    autoInputCode(smsMsg.getSmsCode());
                    break;
                }
                case MSG_QUIT_QUEUE:
                    quit();
                    break;
                case MSG_KILL_ME:
                    killBackgroundProcess(BuildConfig.APPLICATION_ID);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported msg type");
            }
        }
    }

    private void showToast(String toast) {
        String text = mAppContext.getString(R.string.cur_verification_code, toast);
        Toast.makeText(mAppContext, text, Toast.LENGTH_LONG).show();
    }

    private void markSmsAsRead(String sender, String body) {
        XLog.d("Marking SMS...");
        boolean result = operateSms(sender, body, OP_MARK_AS_READ);
        if (result) {
            XLog.i("Mark SMS as read succeed");
        } else {
            XLog.i("Mark SMS as read failed");
        }
    }

    private void deleteSms(String sender, String body) {
        XLog.d("Deleting SMS...");
        boolean result = operateSms(sender, body, OP_DELETE);
        if (result) {
            XLog.i("Delete SMS succeed");
        } else {
            XLog.i("Delete SMS failed");
        }
    }

    /**
     * Handle sms according to its operation
     */
    private boolean operateSms(String sender, String body, @SmsOp int smsOp) {
        Cursor cursor = null;
        try {
            if (ContextCompat.checkSelfPermission(mAppContext, Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                XLog.e("Don't have permission to read/write sms");
                return false;
            }
            String[] projection = new String[]{
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.READ,
                    Telephony.Sms.DATE
            };
            // 查看最近5条短信
            String sortOrder = Telephony.Sms.DATE + " desc limit 5";
            Uri uri = Telephony.Sms.CONTENT_URI;
            ContentResolver resolver = mAppContext.getContentResolver();
            cursor = resolver.query(uri, projection, null, null, sortOrder);
            if (cursor == null) {
                XLog.i("Cursor is null");
                return false;
            }
            while (cursor.moveToNext()) {
                String curAddress = cursor.getString(cursor.getColumnIndex("address"));
                int curRead = cursor.getInt(cursor.getColumnIndex("read"));
                String curBody = cursor.getString(cursor.getColumnIndex("body"));
                if (curAddress.equals(sender) && curRead == 0 && curBody.startsWith(body)) {
                    String smsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                    String where = Telephony.Sms._ID + " = ?";
                    String[] selectionArgs = new String[]{smsMessageId};
                    if (smsOp == OP_DELETE) {
                        int rows = resolver.delete(uri, where, selectionArgs);
                        if (rows > 0) {
                            return true;
                        }
                    } else if (smsOp == OP_MARK_AS_READ) {
                        ContentValues values = new ContentValues();
                        values.put(Telephony.Sms.READ, true);
                        int rows = resolver.update(uri, values, where, selectionArgs);
                        if (rows > 0) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            XLog.e("Operate SMS failed: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    private void recordSmsMsg(SmsMsg smsMsg) {
        try {
            Uri smsMsgUri = DBProvider.SMS_MSG_CONTENT_URI;

            ContentValues values = new ContentValues();
            values.put(SmsMsgDao.Properties.Body.columnName, smsMsg.getBody());
            values.put(SmsMsgDao.Properties.Company.columnName, smsMsg.getCompany());
            values.put(SmsMsgDao.Properties.Date.columnName, smsMsg.getDate());
            values.put(SmsMsgDao.Properties.Sender.columnName, smsMsg.getSender());
            values.put(SmsMsgDao.Properties.SmsCode.columnName, smsMsg.getSmsCode());

            ContentResolver resolver = mAppContext.getContentResolver();
            resolver.insert(smsMsgUri, values);

            String[] projections = {SmsMsgDao.Properties.Id.columnName};
            String order = SmsMsgDao.Properties.Date.columnName + " ASC";
            Cursor cursor = resolver.query(smsMsgUri, projections, null, null, order);
            if (cursor == null) {
                return;
            }
            int count = cursor.getCount();
            int maxRecordCount = PrefConst.MAX_SMS_RECORDS_COUNT_DEFAULT;
            if (cursor.getCount() > maxRecordCount) {
                // 删除最早的记录，直至剩余数目为 PrefConst.MAX_SMS_RECORDS_COUNT_DEFAULT
                ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                String selection = SmsMsgDao.Properties.Id.columnName + " = ?";
                for (int i = 0; i < count - maxRecordCount; i++) {
                    cursor.moveToNext();
                    long id = cursor.getLong(cursor.getColumnIndex(SmsMsgDao.Properties.Id.columnName));
                    ContentProviderOperation operation = ContentProviderOperation.newDelete(smsMsgUri)
                            .withSelection(selection, new String[]{String.valueOf(id)})
                            .build();

                    operations.add(operation);
                }

                resolver.applyBatch(DBProvider.AUTHORITY, operations);
            }

            cursor.close();
        } catch (Exception e) {
            XLog.e("add SMS message record failed", e);
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
                XLog.d("kill %s background process succeed", packageName);
            }
        } catch (Throwable e) {
            XLog.e("error occurs when kill background process %s", packageName, e);
        }
    }

    private void quit() {
        if (workerHandler != null) {
            workerHandler.getLooper().quitSafely();
        }
    }

    // auto-input
    private void autoInputCode(String code) {
        try {
            sendText(code);
            XLog.d("auto input code succeed");
        } catch (Throwable throwable) {
            XLog.e("error occurs when auto input code", throwable);
        }
    }

    /**
     * refer: com.android.commands.input.Input#sendText()
     *
     * @throws Throwable throwable throws if the caller has no android.permission.INJECT_EVENTS permission
     */
    private void sendText(String text) throws Throwable {
        int source = InputDevice.SOURCE_KEYBOARD;

        StringBuilder sb = new StringBuilder(text);

        boolean escapeFlag = false;
        for (int i = 0; i < sb.length(); i++) {
            if (escapeFlag) {
                escapeFlag = false;
                if (sb.charAt(i) == 's') {
                    sb.setCharAt(i, ' ');
                    sb.deleteCharAt(--i);
                }
            }
            if (sb.charAt(i) == '%') {
                escapeFlag = true;
            }
        }

        char[] chars = sb.toString().toCharArray();

        KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        KeyEvent[] events = kcm.getEvents(chars);
        for (KeyEvent keyEvent : events) {
            if (source != keyEvent.getSource()) {
                keyEvent.setSource(source);
            }
            injectKeyEvent(keyEvent);
        }
    }

    /**
     * refer com.android.commands.input.Input#injectKeyEvent()
     */
    @SuppressLint("PrivateApi")
    private void injectKeyEvent(KeyEvent keyEvent) throws Throwable {
        InputManager inputManager = (InputManager) XposedHelpers.callStaticMethod(InputManager.class, "getInstance");

        int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH =
                XposedHelpers.getStaticIntField(InputManager.class, "INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH");

        Class<?>[] paramTypes = {KeyEvent.class, int.class,};
        Object[] args = {keyEvent, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH,};

        XposedHelpers.callMethod(inputManager, "injectInputEvent", paramTypes, args);
    }

}