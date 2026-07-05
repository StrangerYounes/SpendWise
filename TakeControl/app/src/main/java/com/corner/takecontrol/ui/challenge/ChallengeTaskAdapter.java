package com.corner.takecontrol.ui.challenge;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskProgress;
import com.corner.takecontrol.data.model.TaskType;
import com.corner.takecontrol.util.PeriodKeyUtil;
import com.corner.takecontrol.util.ProgressCalculator;
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
        } else if (progress != null && task.getTaskTypeEnum() != TaskType.CHECKMARK) {
            holder.progressStatusText.setText(String.format(Locale.US, "Progress: %.0f / %.0f %s",
                    progress.getValue(), task.getTargetValue(), task.getUnit() != null ? task.getUnit() : ""));
        } else {
            holder.progressStatusText.setText("Not completed yet");
        }

        holder.completeButton.setVisibility(View.GONE);
        holder.logButton.setVisibility(View.GONE);

        if (!readOnly && !complete) {
            if (task.getTaskTypeEnum() == TaskType.CHECKMARK) {
                holder.completeButton.setVisibility(View.VISIBLE);
                holder.completeButton.setOnClickListener(v -> {
                    TaskProgress newProgress = progress != null ? progress
                            : new TaskProgress(getUserId(), task.getId(), periodKey, 0, true);
                    newProgress.setCompleted(true);
                    listener.onComplete(task, newProgress);
                });
            } else {
                holder.logButton.setVisibility(View.VISIBLE);
                holder.logButton.setOnClickListener(v -> showLogDialog(holder.itemView, task, progress, periodKey));
            }
        }
    }

    private void showLogDialog(View anchor, ChallengeTask task, TaskProgress progress, String periodKey) {
        EditText input = new EditText(anchor.getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(anchor.getContext().getString(R.string.enter_value));
        if (progress != null) {
            input.setText(String.valueOf((int) progress.getValue()));
        }

        new AlertDialog.Builder(anchor.getContext())
                .setTitle(task.getTitle())
                .setView(input)
                .setPositiveButton(R.string.log_progress, (dialog, which) -> {
                    String valueStr = input.getText().toString().trim();
                    if (valueStr.isEmpty()) {
                        Toast.makeText(anchor.getContext(), "Enter a value", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double value = Double.parseDouble(valueStr);
                    TaskProgress newProgress = progress != null ? progress
                            : new TaskProgress(getUserId(), task.getId(), periodKey, value, false);
                    newProgress.setValue(value);
                    newProgress.setCompleted(value >= task.getTargetValue());
                    listener.onLogProgress(task, newProgress, value);
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
        final TextView taskTitleText;
        final TextView taskMetaText;
        final TextView progressStatusText;
        final View completeButton;
        final View logButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitleText = itemView.findViewById(R.id.taskTitleText);
            taskMetaText = itemView.findViewById(R.id.taskMetaText);
            progressStatusText = itemView.findViewById(R.id.progressStatusText);
            completeButton = itemView.findViewById(R.id.completeButton);
            logButton = itemView.findViewById(R.id.logButton);
        }
    }
}
