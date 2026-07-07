package com.corner.takecontrol.ui.challenge;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskProgress;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

public class FocusTimerDialogFragment extends DialogFragment {

    public interface OnTimerFinishedListener {
        void onTimerFinished(ChallengeTask task, TaskProgress progress, double minutes);
    }

    private OnTimerFinishedListener listener;
    private ChallengeTask task;
    private TaskProgress progress;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private long totalTimeInMillis;
    private boolean isRunning;

    public static FocusTimerDialogFragment newInstance(ChallengeTask task, TaskProgress progress) {
        FocusTimerDialogFragment fragment = new FocusTimerDialogFragment();
        fragment.task = task;
        fragment.progress = progress;
        return fragment;
    }

    public void setOnTimerFinishedListener(OnTimerFinishedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_focus_timer, null);
        TextView timerText = view.findViewById(R.id.timerText);
        CircularProgressIndicator progressIndicator = view.findViewById(R.id.timerProgress);
        
        double target = task.getTargetValue();
        double current = progress != null ? progress.getValue() : 0;
        double remaining = Math.max(0, target - current);
        
        totalTimeInMillis = (long) (remaining * 60 * 1000);
        timeLeftInMillis = totalTimeInMillis;
        
        updateTimerText(timerText, progressIndicator);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(task.getTitle())
                .setView(view)
                .setPositiveButton(R.string.start_timer, null)
                .setNegativeButton(R.string.cancel, (dialog, which) -> stopTimer());

        Dialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
                if (isRunning) {
                    pauseTimer();
                    ((TextView) v).setText(R.string.resume);
                } else {
                    startTimer(timerText, progressIndicator, (TextView) v);
                    ((TextView) v).setText(R.string.pause);
                }
            });
        });

        return dialog;
    }

    private void startTimer(TextView timerText, CircularProgressIndicator progressIndicator, TextView actionButton) {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText(timerText, progressIndicator);
            }

            @Override
            public void onFinish() {
                isRunning = false;
                timeLeftInMillis = 0;
                updateTimerText(timerText, progressIndicator);
                if (listener != null) {
                    double minutesFinished = (double) totalTimeInMillis / (60 * 1000);
                    listener.onTimerFinished(task, progress, minutesFinished);
                }
                dismiss();
            }
        }.start();
        isRunning = true;
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void updateTimerText(TextView timerText, CircularProgressIndicator progressIndicator) {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        timerText.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
        
        if (totalTimeInMillis > 0) {
            int progress = (int) (timeLeftInMillis * 100 / totalTimeInMillis);
            progressIndicator.setProgress(progress);
        }
    }
}
