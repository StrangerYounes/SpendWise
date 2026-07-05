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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChallengeTaskAdapter extends RecyclerView.Adapter<ChallengeTaskAdapter.ViewHolder> {

    public interface TaskActionListener {
        void onComplete(ChallengeTask task, TaskProgress progress);

        void onLogProgress(ChallengeTask task, TaskProgress progress, double value);
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

        if (complete) {
            holder.progressStatusText.setText("Completed for this period");
            holder.progressStatusText.setTextColor(holder.itemView.getContext().getColor(R.color.task_completed));
            holder.taskCard.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.task_completed_bg));
            holder.taskCard.setStrokeColor(holder.itemView.getContext().getColor(R.color.task_completed));
        } else if (progress != null && task.getTaskTypeEnum() != TaskType.CHECKMARK && progress.getValue() > 0) {
            holder.progressStatusText.setText(String.format(Locale.US, "Progress: %.0f / %.0f %s",
                    progress.getValue(), task.getTargetValue(), task.getUnit() != null ? task.getUnit() : ""));
            holder.progressStatusText.setTextColor(holder.itemView.getContext().getColor(R.color.task_partial));
            holder.taskCard.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.task_partial_bg));
            holder.taskCard.setStrokeColor(holder.itemView.getContext().getColor(R.color.task_partial));
        } else {
            holder.progressStatusText.setText("Not completed yet");
            holder.progressStatusText.setTextColor(holder.itemView.getContext().getColor(R.color.task_not_started));
            holder.taskCard.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.task_not_started_bg));
            holder.taskCard.setStrokeColor(holder.itemView.getContext().getColor(android.R.color.transparent));
        }

        holder.completeButton.setVisibility(View.GONE);
        holder.logButton.setVisibility(View.GONE);

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
                                : new TaskProgress(getUserId(), task.getId(), periodKey, 0, true);
                        newProgress.setCompleted(true);
                        listener.onComplete(task, newProgress);
                    });
                }
            } else {
                holder.logButton.setVisibility(View.VISIBLE);
                holder.logButton.setOnClickListener(v -> showLogDialog(holder.itemView, task, progress, periodKey));
            }
        }
    }

    private void showLogDialog(View anchor, ChallengeTask task, TaskProgress progress, String periodKey) {
        View dialogView = LayoutInflater.from(anchor.getContext()).inflate(R.layout.dialog_log_progress, null);
        TextView progressInfoText = dialogView.findViewById(R.id.currentProgressText);
        TextInputEditText valueInput = dialogView.findViewById(R.id.logValueInput);

        double currentVal = progress != null ? progress.getValue() : 0;
        String unit = task.getUnit() != null ? task.getUnit() : "";
        progressInfoText.setText(String.format(Locale.US, "Current progress: %.1f / %.1f %s",
                currentVal, task.getTargetValue(), unit));

        new MaterialAlertDialogBuilder(anchor.getContext())
                .setTitle(task.getTitle())
                .setView(dialogView)
                .setPositiveButton(R.string.log_progress, (dialog, which) -> {
                    String valueStr = valueInput.getText() != null ? valueInput.getText().toString().trim() : "";
                    if (valueStr.isEmpty()) {
                        return;
                    }
                    try {
                        double increment = Double.parseDouble(valueStr);
                        double newValue = currentVal + increment;

                        TaskProgress newProgress = progress != null ? progress
                                : new TaskProgress(getUserId(), task.getId(), periodKey, newValue, false);
                        newProgress.setValue(newValue);
                        newProgress.setCompleted(newValue >= task.getTargetValue());

                        listener.onLogProgress(task, newProgress, increment);
                    } catch (NumberFormatException e) {
                        Toast.makeText(anchor.getContext(), "Invalid value", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String buildMeta(ChallengeTask task, String periodKey) {
        return PeriodKeyUtil.getPeriodLabel(task.getFrequencyEnum(), periodKey)
                + " · " + task.getFrequencyEnum().name().toLowerCase(Locale.US);
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
        final View completeButton;
        final View logButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskCard = itemView.findViewById(R.id.taskCard);
            taskTitleText = itemView.findViewById(R.id.taskTitleText);
            taskMetaText = itemView.findViewById(R.id.taskMetaText);
            progressStatusText = itemView.findViewById(R.id.progressStatusText);
            completeButton = itemView.findViewById(R.id.completeButton);
            logButton = itemView.findViewById(R.id.logButton);
        }
    }
}
