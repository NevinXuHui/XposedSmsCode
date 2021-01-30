package com.nevinxu.xsmscode.ui.record;

import com.nevinxu.xsmscode.common.mvp.BasePresenter;
import com.nevinxu.xsmscode.common.mvp.BaseView;
import com.nevinxu.xsmscode.data.db.entity.SmsMsg;

import java.util.List;

interface CodeRecordContract {

    interface View extends BaseView {

        void showRefreshing();

        void stopRefresh();

        void displayData(List<SmsMsg> smsMsgList);

    }

    interface Presenter extends BasePresenter<View> {

        void loadData();

        void removeSmsMsg(List<SmsMsg> smsMsgList);
    }

}
