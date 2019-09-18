package com.studio4plus.homerplayer.analytics;

import android.content.Context;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinishedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;
import com.studio4plus.homerplayer.events.PlaybackErrorEvent;
import com.studio4plus.homerplayer.events.PlaybackProgressedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppingEvent;
import com.studio4plus.homerplayer.events.SettingsEnteredEvent;
import com.studio4plus.homerplayer.model.AudioBook;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class AnalyticsTracker {
    private static final String BOOKS_INSTALLED = "booksInstalled";
    private static final String BOOKS_INSTALLED_TYPE_KEY = "type";
    private static final String BOOK_PLAYED = "bookPlayed";
    private static final String BOOK_PLAYED_TYPE_KEY = "type";
    private static final String BOOK_PLAYED_DURATION_KEY = "durationBucket";
    private static final String BOOK_PLAYED_TYPE_SAMPLE = "sample";
    private static final String BOOK_PLAYED_TYPE_USER_CONTENT = "userContent";
    private static final String BOOK_SWIPED = "bookSwiped";
    private static final String BOOK_LIST_DISPLAYED = "bookListDisplayed";
    private static final String FF_REWIND = "ffRewind";
    private static final String FF_REWIND_ABORTED = "ffRewindAborted";
    private static final String FF_REWIND_IS_FF_KEY = "isFf";
    private static final String PERMISSION_RATIONALE_SHOWN = "permissionRationaleShown";
    private static final String PERMISSION_RATIONALE_REQUEST_KEY = "permissionRequest";
    private static final String PLAYBACK_ERROR = "playbackError";
    private static final String PLAYBACK_ERROR_MESSAGE_KEY = "message";
    private static final String PLAYBACK_ERROR_FORMAT_KEY = "format";
    private static final String PLAYBACK_ERROR_DURATION_KEY = "durationMs";
    private static final String PLAYBACK_ERROR_POSITION_KEY = "positionMs";
    private static final String SAMPLES_DOWNLOAD_STARTED = "samplesDownloadStarted";
    private static final String SAMPLES_DOWNLOAD_SUCCESS = "samplesDownloadSuccess";
    private static final String SAMPLES_DOWNLOAD_FAILURE = "samplesDownloadFailure";

    private static final NavigableMap<Long, String> PLAYBACK_DURATION_BUCKETS = new TreeMap<>();

    private final GlobalSettings globalSettings;
    private final StatsLogger stats;

    private CurrentlyPlayed currentlyPlayed;

    @Inject
    public AnalyticsTracker(
            Context context, GlobalSettings globalSettings, EventBus eventBus) {
        this.globalSettings = globalSettings;
        eventBus.register(this);

        // Not bothering with injecting the stats logger, at least until I need to add a debug
        // implementation.
        stats = new StatsLogger(context);
    }

    @Subscribe
    public void onEvent(AudioBooksChangedEvent event) {
        if (event.contentType.supersedes(globalSettings.booksEverInstalled())) {
            Map<String, String> data = Collections.singletonMap(
                    BOOKS_INSTALLED_TYPE_KEY, event.contentType.name());
            stats.logEvent(BOOKS_INSTALLED, data);
        }
        globalSettings.setBooksEverInstalled(event.contentType);
    }

    @Subscribe
    public void onEvent(SettingsEnteredEvent event) {
        globalSettings.setSettingsEverEntered();
    }

    @Subscribe
    public void onEvent(DemoSamplesInstallationStartedEvent event) {
        stats.logEvent(SAMPLES_DOWNLOAD_STARTED);
    }

    @Subscribe
    public void onEvent(DemoSamplesInstallationFinishedEvent event) {
        if (event.success) {
            stats.logEvent(SAMPLES_DOWNLOAD_SUCCESS);
        } else {
            Map<String, String> data = Collections.singletonMap("error", event.errorMessage);
            stats.logEvent(SAMPLES_DOWNLOAD_FAILURE, data);
        }
    }

    @Subscribe
    public void onEvent(PlaybackProgressedEvent event) {
        if (currentlyPlayed == null)
            currentlyPlayed = new CurrentlyPlayed(event.audioBook, System.nanoTime());
    }

    @Subscribe
    public void onEvent(PlaybackStoppingEvent event) {
        if (currentlyPlayed != null) {
            Map<String, String> data = new TreeMap<>();
            data.put(BOOK_PLAYED_TYPE_KEY,
                     currentlyPlayed.audioBook.isDemoSample()
                             ? BOOK_PLAYED_TYPE_SAMPLE
                             : BOOK_PLAYED_TYPE_USER_CONTENT);
            long elapsedTimeS = TimeUnit.NANOSECONDS.toSeconds(
                    System.nanoTime() - currentlyPlayed.startTimeNano);
            Map.Entry<Long, String> bucket = PLAYBACK_DURATION_BUCKETS.floorEntry(elapsedTimeS);
            data.put(BOOK_PLAYED_DURATION_KEY, bucket.getValue());
            currentlyPlayed = null;
            stats.logEvent(BOOK_PLAYED, data);
        }
    }

    @Subscribe
    public void onEvent(PlaybackErrorEvent event) {
        Map<String, String> data = new TreeMap<>();
        data.put(PLAYBACK_ERROR_MESSAGE_KEY, event.errorMessage);
        data.put(PLAYBACK_ERROR_FORMAT_KEY, event.format);
        data.put(PLAYBACK_ERROR_DURATION_KEY, event.durationMs + "ms");
        data.put(PLAYBACK_ERROR_POSITION_KEY, event.positionMs + "ms");
        stats.logEvent(PLAYBACK_ERROR, data);
    }

    public void onBookSwiped() {
        stats.logEvent(BOOK_SWIPED);
    }

    public void onBookListDisplayed() {
        stats.logEvent(BOOK_LIST_DISPLAYED);
    }

    public void onFfRewindStarted(boolean isFf) {
        stats.logEvent(FF_REWIND, Collections.singletonMap(FF_REWIND_IS_FF_KEY, Boolean.toString(isFf)));
    }

    public void onFfRewindFinished(long elapsedWallTimeMs) {
        stats.endTimedEvent(FF_REWIND);
    }

    public void onFfRewindAborted() {
        stats.logEvent(FF_REWIND_ABORTED);
    }

    public void onPermissionRationaleShown(String permissionRequest) {
        stats.logEvent(
                PERMISSION_RATIONALE_SHOWN,
                Collections.singletonMap(PERMISSION_RATIONALE_REQUEST_KEY, permissionRequest));
    }

    private static class CurrentlyPlayed {
        final AudioBook audioBook;
        final long startTimeNano;

        private CurrentlyPlayed(AudioBook audioBook, long startTimeNano) {
            this.audioBook = audioBook;
            this.startTimeNano = startTimeNano;
        }
    }

    static {
        PLAYBACK_DURATION_BUCKETS.put(0L, "0 - 30s");
        PLAYBACK_DURATION_BUCKETS.put(30L, "30 - 60s");
        PLAYBACK_DURATION_BUCKETS.put(60L, "1 - 5m");
        PLAYBACK_DURATION_BUCKETS.put(5 * 60L, "5 - 15m");
        PLAYBACK_DURATION_BUCKETS.put(15 * 60L, "15 - 30m");
        PLAYBACK_DURATION_BUCKETS.put(30 * 60L, "> 30m");
    }
}
