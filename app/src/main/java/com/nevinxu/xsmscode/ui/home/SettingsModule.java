package com.nevinxu.xsmscode.ui.home;

import com.nevinxu.xsmscode.ui.app.FragmentScope;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class SettingsModule {

    @FragmentScope
    @Binds
    abstract SettingsContract.View bindView(SettingsFragment view);

    @FragmentScope
    @Binds
    abstract SettingsContract.Presenter bindPresenter(SettingsPresenter presenter);

}
