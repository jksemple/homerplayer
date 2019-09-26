package com.studio4plus.homerplayer.events;

import com.studio4plus.homerplayer.model.AudioBook;

/**
 * Sent to sync elapsed time, right after playback has started and then some time after each full
 * second of playback (in playback time, i.e. affected by playback speed).
 * Contains the audiobook and the current playback position.
 */
public class PlaybackMetadataStringEvent {
    public final String tagId;
    public final String stringValue;

    public PlaybackMetadataStringEvent(String tagId, String stringValue) {
        this.tagId = tagId;
        this.stringValue = stringValue;
    }
}
