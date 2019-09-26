package com.studio4plus.homerplayer.ui.classic;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.ui.FFRewindTimer;
import com.studio4plus.homerplayer.ui.HintOverlay;
import com.studio4plus.homerplayer.ui.PressReleaseDetector;
import com.studio4plus.homerplayer.ui.SimpleAnimatorListener;
import com.studio4plus.homerplayer.ui.UiControllerPlayback;
import com.studio4plus.homerplayer.util.ViewUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

//import io.codetail.animation.ViewAnimationUtils;

public class FragmentPlayback extends Fragment implements FFRewindTimer.Observer {

    private View view;
    private ImageButton stopButton;
    private ImageButton rewindButton;
    private ImageButton ffButton;
    private ImageButton skipForwardButton;
    private ImageButton skipBackwardButton;
    private TextView bookTitle;
    private TextView trackNumber;
    private TextView trackTitle;
    private TextView trackLength;
    private TextView artist;

    private TextView elapsedTimeView;
    private TextView totalLengthView;
    private TextView elapsedTimeRewindFFView;
    private RewindFFHandler rewindFFHandler;
    //private Animator elapsedTimeRewindFFViewAnimation;

    private @Nullable UiControllerPlayback controller;

    @Inject public GlobalSettings globalSettings;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_playback, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        stopButton = view.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> {
            Preconditions.checkNotNull(controller);
            controller.stopPlayback();
        });

        skipForwardButton = view.findViewById(R.id.skipForwardButton);
        skipForwardButton.setOnClickListener(v -> {
            Preconditions.checkNotNull(controller);
            controller.skipForward();
        });
        skipBackwardButton = view.findViewById(R.id.skipBackwardButton);
        skipBackwardButton.setOnClickListener(v -> {
            Preconditions.checkNotNull(controller);
            controller.skipBackward();
        });
        bookTitle = view.findViewById(R.id.bookTitle);
        trackNumber = view.findViewById(R.id.trackNumber);
        trackLength = view.findViewById(R.id.trackLength);
        trackTitle = view.findViewById(R.id.trackTitle);
        artist = view.findViewById(R.id.artist);

        elapsedTimeView = view.findViewById(R.id.elapsedTime);
        totalLengthView = view.findViewById(R.id.totalLength);
        elapsedTimeRewindFFView = view.findViewById(R.id.elapsedTimeRewindFF);

        elapsedTimeView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) elapsedTimeRewindFFView.getLayoutParams();
            params.leftMargin = left;
            params.topMargin = top;
            params.width = right - left;
            params.height = bottom - top;
            elapsedTimeRewindFFView.setLayoutParams(params);
        });

        rewindButton = view.findViewById(R.id.rewindButton);
        ffButton = view.findViewById(R.id.fastForwardButton);

        View rewindFFOverlay = view.findViewById(R.id.rewindFFOverlay);
        rewindFFHandler = new RewindFFHandler(
                (View) rewindFFOverlay.getParent(), rewindFFOverlay);
        rewindButton.setEnabled(false);
        ffButton.setEnabled(false);

        rewindFFOverlay.setOnTouchListener((v, event) -> {
            // Don't let any events "through" the overlay.
            return true;
        });

        //elapsedTimeRewindFFViewAnimation =
        //        AnimatorInflater.loadAnimator(view.getContext(), R.animator.bounce);
        //elapsedTimeRewindFFViewAnimation.setTarget(elapsedTimeRewindFFView);

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();
        Crashlytics.log("UI: FragmentPlayback resumed");
        rewindButton.setOnTouchListener(new PressReleaseDetector(rewindFFHandler));
        ffButton.setOnTouchListener(new PressReleaseDetector(rewindFFHandler));
        showHintIfNecessary();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onPause() {
        // Remove press-release detectors and tell rewindFFHandler directly that we're paused.
        rewindButton.setOnTouchListener(null);
        ffButton.setOnTouchListener(null);
        rewindFFHandler.onPause();
        super.onPause();
    }

    void onPlaybackStopping() {
        disableUiOnStopping();
        rewindFFHandler.onStopping();
    }

    void onPlaybackProgressed(long playbackPositionMs)
    {
        //onTimerUpdated(playbackPositionMs, fileName);
        onTimerUpdated(playbackPositionMs);
        enableUiOnStart();
    }

    void onPlaybackMetadataString(String tagId, String stringValue) {
        switch(tagId) {
            case "Book title":
                bookTitle.setText(stringValue);
                break;
            case "Track title":
                trackTitle.setText(stringValue);
                break;
            case "Artist":
                artist.setText(getString(R.string.author, stringValue));
                break;
            case "Track":
                trackNumber.setText(getString(R.string.trackTitle, stringValue));
                break;
            case "Length":
                trackLength.setText(trackLength(Long.parseLong(stringValue)));
                break;
            case "Total length":
                totalLengthView.setText(totalLength(Long.parseLong(stringValue)));
                break;
        }
    }
    private void enableUiOnStart() {
        rewindButton.setEnabled(true);
        ffButton.setEnabled(true);
    }

    private void disableUiOnStopping() {
        rewindButton.setEnabled(false);
        stopButton.setEnabled(false);
        ffButton.setEnabled(false);
    }

    private String elapsedTime(long elapsedMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs) % 60;

        return getString(R.string.playback_elapsed_time, hours, minutes, seconds);
    }
    private String totalLength(long elapsedMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs) % 60;

        return getString(R.string.playback_total_length, hours, minutes, seconds);
    }
    private String trackLength(long elapsedMs) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs) % 60;

        return getString(R.string.track_length, minutes, seconds);
    }

    private void showHintIfNecessary() {
        if (isResumed() && isVisible()) {
            if (!globalSettings.flipToStopHintShown()) {
                HintOverlay overlay = new HintOverlay(
                        view, R.id.flipToStopHintOverlayStub, R.string.hint_flip_to_stop, R.drawable.hint_flip_to_stop);
                overlay.show();
                globalSettings.setFlipToStopHintShown();
            }
        }
    }

    @Override
    public void onTimerUpdated(long displayTimeMs) {
        elapsedTimeView.setText(elapsedTime(displayTimeMs));
        //elapsedTimeRewindFFView.setText(elapsedTime(displayTimeMs));
        //trackTitle.setText(fileName.replaceFirst("\\.(mp3)", ""));
    }

    @Override
    public void onTimerLimitReached() {
        //if (elapsedTimeRewindFFView.getVisibility() == View.VISIBLE) {
        // elapsedTimeRewindFFViewAnimation.start();
        //}
    }

    void setController(@NonNull UiControllerPlayback controller) {
        this.controller = controller;
    }

    private class RewindFFHandler implements PressReleaseDetector.Listener {

        private final View commonParent;
        private final View rewindOverlay;
        private Animator currentAnimator;
        private boolean isRunning;

        private RewindFFHandler(@NonNull View commonParent, @NonNull View rewindOverlay) {
            this.commonParent = commonParent;
            this.rewindOverlay = rewindOverlay;
        }

        @Override
        public void onPressed(final View v, float x, float y) {
            Preconditions.checkNotNull(controller);
            //if (currentAnimator != null) {
            //    currentAnimator.cancel();
            //}

            final boolean isFF = (v == ffButton);
            //rewindOverlay.setVisibility(View.VISIBLE);
            //currentAnimator = createAnimation(v, x, y, true);
            //currentAnimator.addListener(new SimpleAnimatorListener() {
            //    private boolean isCancelled = false;

            //    @Override
            //    public void onAnimationEnd(Animator animator) {

            //        currentAnimator = null;
            //        if (!isCancelled)
            //            controller.startRewind(isFF, FragmentPlayback.this);
            //    }

            //    @Override
            //    public void onAnimationCancel(Animator animator) {
            //        isCancelled = true;
            //       resumeFromRewind();
            //   }
            //});
            //currentAnimator.start();

            controller.pauseForRewind();
            controller.startRewind(isFF, FragmentPlayback.this);
            isRunning = true;
        }

        @Override
        public void onReleased(View v, float x, float y) {
            //if (currentAnimator != null) {
            //    currentAnimator.cancel();
            //    rewindOverlay.setVisibility(View.GONE);
            //    currentAnimator = null;
            //} else {
            //    currentAnimator = createAnimation(v, x, y, false);
            //    currentAnimator.addListener(new SimpleAnimatorListener() {
            //        @Override
            //        public void onAnimationEnd(Animator animator) {
            //            rewindOverlay.setVisibility(View.GONE);
            //            currentAnimator = null;
            //        }
            //    });
            //    currentAnimator.start();
            //    resumeFromRewind();
            //}
            resumeFromRewind();
        }

        void onPause() {
            //if (currentAnimator != null) {
            //    // Cancelling the animation calls resumeFromRewind.
            //    currentAnimator.cancel();
            //    currentAnimator = null;
            //} else if (isRunning) {
            //    resumeFromRewind();
            //}
            if (isRunning) {
                resumeFromRewind();
            }
        }

        void onStopping() {
            if (isRunning)
                stopRewind();
        }

        private void resumeFromRewind() {
            Preconditions.checkNotNull(controller);
            stopRewind();
            controller.resumeFromRewind();
        }

        private void stopRewind() {
            Preconditions.checkNotNull(controller);
            controller.stopRewind();
            isRunning = false;
        }

        /*
        private Animator createAnimation(View v, float x, float y, boolean reveal) {
            Rect viewRect = ViewUtils.getRelativeRect(commonParent, v);
            float startX = viewRect.left + x;
            float startY = viewRect.top + y;

            // Compute final radius
            float dx = Math.max(startX, commonParent.getWidth() - startX);
            float dy = Math.max(startY, commonParent.getHeight() - startY);
            float finalRadius = (float) Math.hypot(dx, dy);

            float initialRadius = reveal ? 0f : finalRadius;
            if (!reveal)
                finalRadius = 0f;

            final int durationResId = reveal
                    ? R.integer.ff_rewind_overlay_show_animation_time_ms
                    : R.integer.ff_rewind_overlay_hide_animation_time_ms;
            Animator animator = ViewAnimationUtils.createCircularReveal(
                    rewindOverlay, Math.round(startX), Math.round(startY), initialRadius, finalRadius);
            animator.setDuration(getResources().getInteger(durationResId));
            animator.setInterpolator(new AccelerateDecelerateInterpolator());

            return animator;
        }
        */
    }
}
