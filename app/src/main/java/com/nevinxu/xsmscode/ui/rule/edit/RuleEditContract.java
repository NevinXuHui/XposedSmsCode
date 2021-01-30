package com.nevinxu.xsmscode.ui.rule.edit;

import android.os.Bundle;

import com.nevinxu.xsmscode.common.mvp.BasePresenter;
import com.nevinxu.xsmscode.common.mvp.BaseView;
import com.nevinxu.xsmscode.data.db.entity.SmsCodeRule;

interface RuleEditContract {

    interface View extends BaseView {

        void displayCodeRule(SmsCodeRule codeRule);

        void clearAllErrorInfo();

        void onTemplateSaved(boolean success);

        void showErrorInfo(boolean companyValid, boolean keywordValid, boolean codeRegexValid);

        void hideSoftInput();

        void onCodeRuleSaved(boolean success);
    }

    interface Presenter extends BasePresenter<View> {

        void handleArguments(Bundle args);

        void saveAsTemplate(SmsCodeRule template);

        void saveIfValid(SmsCodeRule codeRule);

    }

}
