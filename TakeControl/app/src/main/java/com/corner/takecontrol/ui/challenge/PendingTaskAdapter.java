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

    private final List<ChallengeTask> tasks = new ArrayList<>();
    private final OnRemoveListener removeListener;

    public PendingTaskAdapter(OnRemoveListener removeListener) {
        this.removeListener = removeListener;
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
    }

    private String buildMeta(ChallengeTask task) {
        TaskFrequency frequency = task.getFrequencyEnum();
        TaskType type = task.getTaskTypeEnum();
        String freq = frequency.name().toLowerCase(Locale.US);
        if (type == TaskType.CHECKMARK) {
            return freq + " · checkmark";
        }
        return freq + " · " + (int) task.getTargetValue() + " " + (task.getUnit() != null ? task.getUnit() : "");
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView taskTitleText;
        final TextView taskMetaText;
        final View removeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitleText = itemView.findViewById(R.id.taskTitleText);
            taskMetaText = itemView.findViewById(R.id.taskMetaText);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}
