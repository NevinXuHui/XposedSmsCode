package com.nevinxu.xsmscode.ui.rule;


import com.nevinxu.xsmscode.ui.app.FragmentScope;
import com.nevinxu.xsmscode.ui.rule.edit.RuleEditFragment;
import com.nevinxu.xsmscode.ui.rule.edit.RuleEditModule;
import com.nevinxu.xsmscode.ui.rule.list.RuleListFragment;
import com.nevinxu.xsmscode.ui.rule.list.RuleListModule;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class CodeRulesModule {

    @FragmentScope
    @ContributesAndroidInjector(modules = RuleListModule.class)
    abstract RuleListFragment contributeRuleListFragment();


    @FragmentScope
    @ContributesAndroidInjector(modules = RuleEditModule.class)
    abstract RuleEditFragment contributeRuleEditFragment();

}
