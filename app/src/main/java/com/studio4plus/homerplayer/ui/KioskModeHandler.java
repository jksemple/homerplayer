package com.studio4plus.homerplayer.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.events.KioskModeChanged;

import javax.inject.Inject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

@ActivityScope
public class KioskModeHandler {

    private static final String SCREEN_LOCK_PREFS = "ScreenLocker";
    private static final String PREF_SCREEN_LOCK_ENABLED = "screen_lock_enabled";

    private final Activity activity;
    private final GlobalSettings globalSettings;
    private final EventBus eventBus;
    private boolean keepNavigation = false;

    @Inject
    KioskModeHandler(Activity activity, GlobalSettings settings, EventBus eventBus) {
        this.activity = activity;
        this.globalSettings = settings;
        this.eventBus = eventBus;
    }

    public void setKeepNavigation(Boolean keepNavigation) {
        this.keepNavigation = keepNavigation;
    }

    public void onActivityStart() {
        if (!globalSettings.isFullKioskModeEnabled() && isLockTaskEnabled())
            lockTask(false);
        setUiFlagsAndLockTask();
        eventBus.register(this);
    }

    public void onActivityStop() {
        eventBus.unregister(this);
    }

    public void onFocusGained() {
        setUiFlagsAndLockTask();
    }

    @Subscribe
    public void onEvent(KioskModeChanged event) {
        if (event.type == KioskModeChanged.Type.FULL)
            lockTask(event.isEnabled);
        setNavigationVisibility(!event.isEnabled);
    }

    private void setUiFlagsAndLockTask() {
        int visibilitySetting = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!keepNavigation) {
            visibilitySetting |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }

        activity.getWindow().getDecorView().setSystemUiVisibility(visibilitySetting);
        if (globalSettings.isAnyKioskModeEnabled())
            setNavigationVisibility(false);

        if (globalSettings.isFullKioskModeEnabled())
            lockTask(true);
    }

    private void setNavigationVisibility(boolean show) {
        if (Build.VERSION.SDK_INT < 19 || keepNavigation)
            return;

        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        View decorView = activity.getWindow().getDecorView();
        int visibilitySetting = decorView.getSystemUiVisibility();
        if (show)
            visibilitySetting &= ~flags;
        else
            visibilitySetting |= flags;

        decorView.setSystemUiVisibility(visibilitySetting);
    }

    private boolean isLockTaskEnabled() {
        return activity.getSharedPreferences(SCREEN_LOCK_PREFS, Context.MODE_PRIVATE)
                .getBoolean(PREF_SCREEN_LOCK_ENABLED, false);
    }

    private void lockTask(boolean isLocked) {
        activity.getSharedPreferences(SCREEN_LOCK_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_SCREEN_LOCK_ENABLED, isLocked).apply();
        if (isLocked)
            API21.startLockTask(activity);
        else
            API21.stopLockTask(activity);
    }
    @TargetApi(21)
    private static class API21 {
        static void startLockTask(Activity activity) {
            activity.startLockTask();
        }

        static void stopLockTask(Activity activity) {
            activity.stopLockTask();
        }
    }
}
