package com.nevinxu.xsmscode.ui.home;

import android.os.Bundle;

import com.nevinxu.xsmscode.common.mvp.BasePresenter;
import com.nevinxu.xsmscode.common.mvp.BaseView;
import com.nevinxu.xsmscode.data.db.entity.ApkVersion;

public interface SettingsContract {

    interface View extends BaseView {

        void showGetAlipayPacketDialog();

        void showSmsCodeTestResult(String code);

        void showCheckError(Throwable t);

        void showUpdateDialog(ApkVersion latestVersion);

        void showAppAlreadyNewest();

        void updateUIByModuleStatus(boolean moduleEnabled);
    }

    interface Presenter extends BasePresenter<View> {

        void handleArguments(Bundle args);

        void setPreferenceWorldWritable(String preferencesName);

        void hideOrShowLauncherIcon(boolean hide);

        void performSmsCodeTest(String msgBody);

        void joinQQGroup();

        void showSourceProject();

        void setInternalFilesWritable();

        void checkUpdate();

        void updateFromGithub();

        void updateFromCoolApk();
    }

}
