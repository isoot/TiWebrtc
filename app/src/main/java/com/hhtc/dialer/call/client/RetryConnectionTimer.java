package com.hhtc.dialer.call.client;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

public abstract class RetryConnectionTimer {

    /**
     * Millis since epoch when alarm should stop.
     */
    private static long mMillisInFuture;

    private static long startMillisInFuture;

    /**
     * The interval in millis that the user receives callbacks
     */
    private final long mCountdownInterval;

    private long mStopTimeInFuture;

    /**
     * boolean representing if the timer was cancelled
     */
    private boolean mCancelled = false;

    /**
     * boolean timer start
     */
    private boolean start = false;


    /**
     * @param millisInFuture    The number of millis in the future from the call
     *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
     *                          is called.
     * @param countDownInterval The interval along the way to receive
     *                          {@link #onTick(long)} callbacks.
     */
    public RetryConnectionTimer(long millisInFuture, long countDownInterval) {
        mMillisInFuture = millisInFuture;
        startMillisInFuture = millisInFuture;
        mCountdownInterval = countDownInterval;
    }

    /**
     * Cancel the countdown.
     */
    public synchronized final void cancel() {
        if (start) {
            mCancelled = true;
            mHandler.removeMessages(MSG);
            start = false;
        }
    }

    /**
     * Start the countdown.
     */
    public synchronized final RetryConnectionTimer start() {
        mCancelled = false;
        mMillisInFuture = startMillisInFuture;
        start = true;
        onStart();
        mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
        return this;
    }


    /**
     * Callback fired when the time is start
     */
    public abstract void onStart();

    /**
     * Callback fired on regular interval.
     *
     * @param millisUntilFinished The amount of time until finished.
     */
    public abstract void onTick(long millisUntilFinished);

    /**
     * Callback fired when the time is up.
     */
    public abstract void onFinish();


    private static final int MSG = 1;


    // handles counting down
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            synchronized (RetryConnectionTimer.this) {
                if (mCancelled) {
                    return;
                }

                final long millisLeft = mStopTimeInFuture - SystemClock.elapsedRealtime();

                if (millisLeft <= 0) {
                    start = false;
                    onFinish();
                } else {
                    long lastTickStart = SystemClock.elapsedRealtime();
                    onTick(millisLeft);

                    // take into account user's onTick taking time to execute
                    long lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart;
                    long delay;

                    if (millisLeft < mCountdownInterval) {
                        // just delay until done
                        delay = millisLeft - lastTickDuration;

                        // special case: user's onTick took more than interval to
                        // complete, trigger onFinish without delay
                        if (delay < 0) delay = 0;
                    } else {
                        delay = mCountdownInterval - lastTickDuration;

                        // special case: user's onTick took more than interval to
                        // complete, skip to next interval
                        while (delay < 0) delay += mCountdownInterval;
                    }
                    if (!mCancelled)
                        sendMessageDelayed(obtainMessage(MSG), delay);
                }
            }
        }
    };


    public boolean isStart() {
        return start;
    }

    public abstract void onStop();

    public boolean isCancelled() {
        return mCancelled;
    }
}
