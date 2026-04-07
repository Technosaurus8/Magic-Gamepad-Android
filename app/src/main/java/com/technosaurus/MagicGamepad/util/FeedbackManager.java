package com.technosaurus.MagicGamepad.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Manages touch feedback (sound or vibration).
 * Pre-allocates AudioTrack once and reuses it instead of creating a new one per touch.
 * Also uses modern VibrationEffect API on API 26+.
 */
public class FeedbackManager {

    private static final String TOUCH_FEEDBACK_KEY = "touch_feedback_key";
    private static final int DURATION_MS = 35;
    private static final int SAMPLE_RATE = 44100;
    private static final int FREQUENCY_HZ = 300;

    private final Vibrator vibrator;
    private final String feedbackType;
    private AudioTrack feedbackTrack;

    public FeedbackManager(Context context, SharedPreferences preferences) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        feedbackType = preferences.getString(TOUCH_FEEDBACK_KEY, "Sound");
        if (!"Vibration".equals(feedbackType)) {
            initFeedbackSound();
        }
    }

    private void initFeedbackSound() {
        int numSamples = (int) ((DURATION_MS / 1000.0) * SAMPLE_RATE);
        double[] sample = new double[numSamples];
        byte[] feedbackSound = new byte[2 * numSamples];

        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (SAMPLE_RATE / (double) FREQUENCY_HZ));
        }

        int idx = 0;
        for (double dVal : sample) {
            short val = (short) (dVal * 32767);
            feedbackSound[idx++] = (byte) (val & 0x00ff);
            feedbackSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        feedbackTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, feedbackSound.length,
                AudioTrack.MODE_STATIC);
        feedbackTrack.write(feedbackSound, 0, feedbackSound.length);
    }

    /**
     * Perform tactile/audio feedback based on user preference.
     */
    public void performFeedback() {
        if ("Vibration".equals(feedbackType)) {
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(DURATION_MS,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(DURATION_MS);
                }
            }
        } else {
            if (feedbackTrack != null) {
                try {
                    feedbackTrack.stop();
                } catch (IllegalStateException ignored) {
                }
                feedbackTrack.reloadStaticData();
                feedbackTrack.play();
            }
        }
    }

    /**
     * Release native resources. Call in Activity/Fragment onDestroy.
     */
    public void release() {
        if (feedbackTrack != null) {
            feedbackTrack.release();
            feedbackTrack = null;
        }
    }
}
