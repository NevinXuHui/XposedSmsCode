package com.nevinxu.xsmscode.ui.rule.edit;

import com.nevinxu.xsmscode.ui.app.FragmentScope;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class RuleEditModule {

    @FragmentScope
    @Binds
    abstract RuleEditContract.View bindView(RuleEditFragment view);

    @FragmentScope
    @Binds
    abstract RuleEditContract.Presenter bindPresenter(RuleEditPresenter presenter);

}
