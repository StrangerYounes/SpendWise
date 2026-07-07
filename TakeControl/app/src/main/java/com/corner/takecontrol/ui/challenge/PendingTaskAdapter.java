package com.corner.takecontrol.ui.challenge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskFrequency;
import com.corner.takecontrol.data.model.TaskType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PendingTaskAdapter extends RecyclerView.Adapter<PendingTaskAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    public interface OnEditListener {
        void onEdit(int position, ChallengeTask task);
    }

    private final List<ChallengeTask> tasks = new ArrayList<>();
    private final OnRemoveListener removeListener;
    private final OnEditListener editListener;

    public PendingTaskAdapter(OnRemoveListener removeListener, OnEditListener editListener) {
        this.removeListener = removeListener;
        this.editListener = editListener;
    }

    public void submitList(List<ChallengeTask> list) {
        tasks.clear();
        if (list != null) {
            tasks.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChallengeTask task = tasks.get(position);
        holder.taskTitleText.setText(task.getTitle());
        holder.taskMetaText.setText(buildMeta(task));
        holder.removeButton.setOnClickListener(v -> removeListener.onRemove(holder.getBindingAdapterPosition()));
        holder.editButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                editListener.onEdit(pos, tasks.get(pos));
            }
        });
    }

    private String buildMeta(ChallengeTask task) {
        TaskFrequency frequency = task.getFrequencyEnum();
        TaskType type = task.getTaskTypeEnum();
        String freq = frequency.name().toLowerCase(Locale.US);
        StringBuilder meta = new StringBuilder(freq);

        String days = formatDaysOfWeek(task.getDaysOfWeek());
        if (!days.isEmpty()) {
            meta.append(" (").append(days).append(")");
        }

        if (task.getExecutionTime() != null && !task.getExecutionTime().isEmpty()) {
            meta.append(" at ").append(task.getExecutionTime());
        }

        if (type == TaskType.CHECKMARK) {
            meta.append(" · checkmark");
        } else {
            meta.append(" · ").append((int) task.getTargetValue()).append(" ").append(task.getUnit() != null ? task.getUnit() : "");
        }
        return meta.toString();
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

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView taskTitleText;
        final TextView taskMetaText;
        final View removeButton;
        final View editButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitleText = itemView.findViewById(R.id.taskTitleText);
            taskMetaText = itemView.findViewById(R.id.taskMetaText);
            removeButton = itemView.findViewById(R.id.removeButton);
            editButton = itemView.findViewById(R.id.editButton);
        }
    }
}
