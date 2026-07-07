package com.corner.takecontrol.ui.challenge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskProgress;
import com.corner.takecontrol.data.model.TaskType;
import com.corner.takecontrol.util.PeriodKeyUtil;
import com.corner.takecontrol.util.ProgressCalculator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChallengeTaskAdapter extends RecyclerView.Adapter<ChallengeTaskAdapter.ViewHolder> {

    public interface TaskActionListener {
        void onComplete(ChallengeTask task, TaskProgress progress);

        void onLogProgress(ChallengeTask task, TaskProgress progress, double value);

        void onResetProgress(ChallengeTask task, TaskProgress progress);

        void onStartTimer(ChallengeTask task, TaskProgress progress);
    }

    private final List<ChallengeTask> tasks = new ArrayList<>();
    private final Map<String, TaskProgress> progressMap = new HashMap<>();
    private final TaskActionListener listener;
    private boolean readOnly;

    public ChallengeTaskAdapter(TaskActionListener listener) {
        this.listener = listener;
    }

    public void submitData(List<ChallengeTask> taskList, List<TaskProgress> progressList, boolean readOnly) {
        this.readOnly = readOnly;
        tasks.clear();
        progressMap.clear();
        if (taskList != null) {
            tasks.addAll(taskList);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (progressList != null) {
            for (TaskProgress progress : progressList) {
                if (userId.equals(progress.getUserId())) {
                    progressMap.put(progress.getTaskId() + "_" + progress.getPeriodKey(), progress);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChallengeTask task = tasks.get(position);
        String periodKey = PeriodKeyUtil.getCurrentPeriodKey(task.getFrequencyEnum());
        TaskProgress progress = progressMap.get(task.getId() + "_" + periodKey);
        boolean complete = ProgressCalculator.isTaskCompleteForCurrentPeriod(task, progress);

        holder.taskTitleText.setText(task.getTitle());
        holder.taskMetaText.setText(buildMeta(task, periodKey));

        holder.taskProgressBar.setVisibility(View.GONE);

        if (complete) {
            String status = progress != null ? progress.getStatus() : null;
            boolean isLate = "LATE".equals(status);
            boolean isSkipped = "SKIPPED".equals(status);

            if (isSkipped) {
                holder.progressStatusText.setText(R.string.day_skipped);
                holder.progressStatusText.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
                holder.taskCard.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.surface_variant));
                holder.taskCard.setStrokeColor(holder.itemView.getContext().getColor(R.color.outline));
            } else if (isLate) {
                holder.progressStatusText.setText(holder.itemView.getContext().getString(
                        R.string.task_completed_late,
                        holder.itemView.getContext().getString(R.string.task_completed_period),
                        holder.itemView.getContext().getString(R.string.status_late)));
                holder.progressStatusText.setTextColor(holder.itemView.getContext().getColor(R.color.task_completed_late));
                holder.taskCard.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.task_completed_late_bg));
                holder.taskCard.setStrokeColor(holder.itemView.getContext().getColor(R.color.task_completed_late));
            } else {
                holder.progressStatusText.setText(R.string.task_completed_period);
                holder.progressStatusText.setTextColor(holder.itemView.getContext().getColor(R.color.task_completed));
                holder.taskCard.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.task_completed_bg));
                holder.taskCard.setStrokeColor(holder.itemView.getContext().getColor(R.color.task_completed));
            }
        } else if (progress != null && task.getTaskTypeEnum() != TaskType.CHECKMARK && progress.getValue() > 0) {
            holder.progressStatusText.setText(holder.itemView.getContext().getString(
                    R.string.task_progress_format,
                    progress.getValue(), task.getTargetValue(),
                    task.getUnit() != null ? task.getUnit() : ""));
            holder.progressStatusText.setTextColor(holder.itemView.getContext().getColor(R.color.task_partial));
            holder.taskCard.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.task_partial_bg));
            holder.taskCard.setStrokeColor(holder.itemView.getContext().getColor(R.color.task_partial));

            if (task.getTargetValue() > 0) {
                int percent = (int) Math.min(100, (progress.getValue() / task.getTargetValue()) * 100);
                holder.taskProgressBar.setVisibility(View.VISIBLE);
                holder.taskProgressBar.setProgress(percent);
                holder.taskProgressBar.setIndicatorColor(holder.itemView.getContext().getColor(R.color.task_partial));
                holder.taskProgressBar.setTrackColor(holder.itemView.getContext().getColor(R.color.task_partial_track));
            }
        } else {
            holder.progressStatusText.setText(R.string.task_not_completed);
            holder.progressStatusText.setTextColor(holder.itemView.getContext().getColor(R.color.task_not_started));
            holder.taskCard.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.task_not_started_bg));
            holder.taskCard.setStrokeColor(holder.itemView.getContext().getColor(android.R.color.transparent));
        }

        holder.completeButton.setVisibility(View.GONE);
        holder.logButton.setVisibility(View.GONE);
        holder.timerButton.setVisibility(View.GONE);

        if (!readOnly) {
            if (task.getTaskTypeEnum() == TaskType.CHECKMARK) {
                holder.completeButton.setVisibility(View.VISIBLE);
                MaterialButton btn = (MaterialButton) holder.completeButton;
                if (complete) {
                    btn.setText(R.string.undo);
                    btn.setOnClickListener(v -> {
                        if (progress != null) {
                            progress.setCompleted(false);
                            listener.onComplete(task, progress);
                        }
                    });
                } else {
                    btn.setText(R.string.mark_complete);
                    btn.setOnClickListener(v -> {
                        TaskProgress newProgress = progress != null ? progress
                                : new TaskProgress(getUserId(), task.getId(), periodKey, 0, true, determineStatus(task));
                        newProgress.setCompleted(true);
                        newProgress.setStatus(determineStatus(task));
                        listener.onComplete(task, newProgress);
                    });
                }
            } else {
                holder.logButton.setVisibility(View.VISIBLE);
                holder.logButton.setOnClickListener(v -> showLogDialog(holder.itemView, task, progress, periodKey));
                
                if (task.getTaskTypeEnum() == TaskType.DURATION && !complete) {
                    holder.timerButton.setVisibility(View.VISIBLE);
                    holder.timerButton.setOnClickListener(v -> listener.onStartTimer(task, 
                            progress != null ? progress : new TaskProgress(getUserId(), task.getId(), periodKey, 0, false, "NORMAL")));
                }
            }
        }
    }

    private void showLogDialog(View anchor, ChallengeTask task, TaskProgress progress, String periodKey) {
        View dialogView = LayoutInflater.from(anchor.getContext()).inflate(R.layout.dialog_log_progress, null);
        TextView progressInfoText = dialogView.findViewById(R.id.currentProgressText);
        TextInputEditText valueInput = dialogView.findViewById(R.id.logValueInput);
        View resetButton = dialogView.findViewById(R.id.resetButton);

        double currentVal = progress != null ? progress.getValue() : 0;
        String unit = task.getUnit() != null ? task.getUnit() : "";
        progressInfoText.setText(String.format(Locale.US, "Current progress: %.1f / %.1f %s",
                currentVal, task.getTargetValue(), unit));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(anchor.getContext())
                .setTitle(task.getTitle())
                .setView(dialogView)
                .setPositiveButton(R.string.log_progress, (dialogInterface, which) -> {
                    String valueStr = valueInput.getText() != null ? valueInput.getText().toString().trim() : "";
                    if (valueStr.isEmpty()) {
                        return;
                    }
                    try {
                        double increment = Double.parseDouble(valueStr);
                        double newValue = Math.max(0, currentVal + increment);

                        TaskProgress newProgress = progress != null ? progress
                                : new TaskProgress(getUserId(), task.getId(), periodKey, newValue, false, "NORMAL");
                        newProgress.setValue(newValue);
                        boolean isNowComplete = newValue >= task.getTargetValue();
                        newProgress.setCompleted(isNowComplete);
                        if (isNowComplete && (progress == null || !progress.isCompleted())) {
                            newProgress.setStatus(determineStatus(task));
                        } else if (!isNowComplete) {
                            newProgress.setStatus("NORMAL");
                        }

                        listener.onLogProgress(task, newProgress, increment);
                    } catch (NumberFormatException e) {
                        Toast.makeText(anchor.getContext(), "Invalid value", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        resetButton.setVisibility(progress != null && (progress.getValue() > 0 || progress.isCompleted()) ? View.VISIBLE : View.GONE);
        resetButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(anchor.getContext())
                    .setTitle(R.string.confirm_reset_title)
                    .setMessage(R.string.confirm_reset_message)
                    .setPositiveButton(R.string.reset, (d, w) -> {
                        dialog.dismiss();
                        if (progress != null) {
                            progress.setValue(0);
                            progress.setCompleted(false);
                            progress.setStatus("NORMAL");
                            listener.onResetProgress(task, progress);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        dialog.show();
    }

    private String buildMeta(ChallengeTask task, String periodKey) {
        StringBuilder sb = new StringBuilder();
        sb.append(PeriodKeyUtil.getPeriodLabel(task.getFrequencyEnum(), periodKey));
        sb.append(" · ");
        sb.append(task.getFrequencyEnum().name().toLowerCase(Locale.US));

        String days = formatDaysOfWeek(task.getDaysOfWeek());
        if (!days.isEmpty()) {
            sb.append(" (").append(days).append(")");
        }

        if (task.getExecutionTime() != null && !task.getExecutionTime().isEmpty()) {
            sb.append(" at ").append(task.getExecutionTime());
        }
        return sb.toString();
    }

    private String formatDaysOfWeek(List<Integer> days) {
        if (days == null || days.isEmpty()) return "";
        String[] shortDays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            int day = days.get(i);
            if (day >= 1 && day <= 7) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(shortDays[day - 1]);
            }
        }
        return sb.toString();
    }

    private String determineStatus(ChallengeTask task) {
        if (task.getExecutionTime() == null || task.getExecutionTime().isEmpty()) {
            return "NORMAL";
        }
        Calendar now = Calendar.getInstance();
        int nowHour = now.get(Calendar.HOUR_OF_DAY);
        int nowMin = now.get(Calendar.MINUTE);

        String[] parts = task.getExecutionTime().split(":");
        if (parts.length == 2) {
            try {
                int targetHour = Integer.parseInt(parts[0]);
                int targetMin = Integer.parseInt(parts[1]);
                if (nowHour > targetHour || (nowHour == targetHour && nowMin > targetMin)) {
                    return "LATE";
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return "NORMAL";
    }

    private String getUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final com.google.android.material.card.MaterialCardView taskCard;
        final TextView taskTitleText;
        final TextView taskMetaText;
        final TextView progressStatusText;
        final LinearProgressIndicator taskProgressBar;
        final View completeButton;
        final View logButton;
        final View timerButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskCard = itemView.findViewById(R.id.taskCard);
            taskTitleText = itemView.findViewById(R.id.taskTitleText);
            taskMetaText = itemView.findViewById(R.id.taskMetaText);
            progressStatusText = itemView.findViewById(R.id.progressStatusText);
            taskProgressBar = itemView.findViewById(R.id.taskProgressBar);
            completeButton = itemView.findViewById(R.id.completeButton);
            logButton = itemView.findViewById(R.id.logButton);
            timerButton = itemView.findViewById(R.id.timerButton);
        }
    }
}
