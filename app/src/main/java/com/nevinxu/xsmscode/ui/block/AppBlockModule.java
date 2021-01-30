package com.nevinxu.xsmscode.ui.block;

import com.nevinxu.xsmscode.ui.app.FragmentScope;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class AppBlockModule {

    @FragmentScope
    @Binds
    abstract AppBlockContract.View bindView(AppBlockFragment view);


    @FragmentScope
    @Binds
    abstract AppBlockContract.Presenter bindPresenter(AppBlockPresenter presenter);

}
