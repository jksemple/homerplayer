package com.studio4plus.homerplayer.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.studio4plus.homerplayer.GlobalSettings;

import dagger.Module;
import dagger.Provides;
import org.greenrobot.eventbus.EventBus;

@Module
public class ActivityModule {
    private final AppCompatActivity activity;

    public ActivityModule(@NonNull AppCompatActivity activity) {
        this.activity = activity;
    }

    @Provides @ActivityScope
    AppCompatActivity activity() {
        return activity;
    }

    @Provides @ActivityScope
    KioskModeHandler provideKioskModeHandler(
            AppCompatActivity activity, GlobalSettings settings, EventBus eventBus) {
        return new KioskModeHandler(activity, settings, eventBus);
    }
}
