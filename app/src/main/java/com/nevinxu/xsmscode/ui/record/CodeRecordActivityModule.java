package com.nevinxu.xsmscode.ui.record;

import com.nevinxu.xsmscode.ui.app.FragmentScope;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class CodeRecordActivityModule {

    @FragmentScope
    @ContributesAndroidInjector(modules = CodeRecordFragmentModule.class)
    abstract CodeRecordFragment contributeCodeRecordFragment();

}
