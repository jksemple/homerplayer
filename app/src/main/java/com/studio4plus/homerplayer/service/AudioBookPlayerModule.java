package com.studio4plus.homerplayer.service;

import android.content.Context;

import com.studio4plus.homerplayer.player.Player;

import dagger.Module;
import dagger.Provides;
import org.greenrobot.eventbus.EventBus;

@Module
public class AudioBookPlayerModule {
    @Provides
    Player provideAudioBookPlayer(Context context, EventBus eventBus) {
        return new Player(context, eventBus);
    }
}
