package com.studio4plus.homerplayer.ui;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.studio4plus.homerplayer.battery.BatteryStatus;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.battery.ChargeLevel;
import com.studio4plus.homerplayer.events.BatteryStatusChangeEvent;

import java.util.EnumMap;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class BatteryStatusIndicator {

    private final ImageView indicatorView;
    private final EventBus eventBus;

    private static final EnumMap<ChargeLevel, Integer> BATTERY_DRAWABLE =
            new EnumMap<>(ChargeLevel.class);

    private static final EnumMap<ChargeLevel, Integer> CHARGING_DRAWABLE =
            new EnumMap<>(ChargeLevel.class);

    static {
        BATTERY_DRAWABLE.put(ChargeLevel.CRITICAL, R.drawable.battery_critical);
        BATTERY_DRAWABLE.put(ChargeLevel.LEVEL_1, R.drawable.battery_red_1);

        CHARGING_DRAWABLE.put(ChargeLevel.CRITICAL, R.drawable.battery_charging_0);
        CHARGING_DRAWABLE.put(ChargeLevel.LEVEL_1, R.drawable.battery_charging_0);
        CHARGING_DRAWABLE.put(ChargeLevel.LEVEL_2, R.drawable.battery_charging_1);
        CHARGING_DRAWABLE.put(ChargeLevel.LEVEL_3, R.drawable.battery_charging_2);
        CHARGING_DRAWABLE.put(ChargeLevel.FULL, R.drawable.battery_3);
    }

    public BatteryStatusIndicator(ImageView indicatorView, EventBus eventBus) {
        this.indicatorView = indicatorView;
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    public void startAnimations() {
        Drawable indicatorDrawable = indicatorView.getDrawable();
        if (indicatorDrawable instanceof AnimationDrawable)
            ((AnimationDrawable) indicatorDrawable).start();
    }

    public void shutdown() {
        // TODO: find an automatic way to unregister
        eventBus.unregister(this);
    }

    private void updateBatteryStatus(BatteryStatus batteryStatus) {
        Integer statusDrawable = batteryStatus.isCharging
                ? CHARGING_DRAWABLE.get(batteryStatus.chargeLevel)
                : BATTERY_DRAWABLE.get(batteryStatus.chargeLevel);

        if (statusDrawable == null) {
            indicatorView.setVisibility(View.GONE);
        } else {
            if (indicatorView.getVisibility() != View.VISIBLE)
                indicatorView.setVisibility(View.VISIBLE);
            indicatorView.setImageResource(statusDrawable);
            startAnimations();
        }
    }

    @Subscribe(sticky = true)
    public void onEvent(BatteryStatusChangeEvent batteryEvent) {
        updateBatteryStatus(batteryEvent.batteryStatus);
    }
}
