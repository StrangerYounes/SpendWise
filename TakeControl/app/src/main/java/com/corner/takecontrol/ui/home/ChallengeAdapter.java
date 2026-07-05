package com.corner.takecontrol.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeStatus;

import java.util.ArrayList;
import java.util.List;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ViewHolder> {

    public interface OnChallengeClickListener {
        void onChallengeClick(Challenge challenge);
    }

    private final List<Challenge> challenges = new ArrayList<>();
    private final OnChallengeClickListener listener;

    public ChallengeAdapter(OnChallengeClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Challenge> list) {
        challenges.clear();
        if (list != null) {
            challenges.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Challenge challenge = challenges.get(position);
        holder.titleText.setText(challenge.getTitle());
        holder.descriptionText.setText(challenge.getDescription() != null ? challenge.getDescription() : "");

        ChallengeStatus status = challenge.getStatusEnum();
        holder.statusText.setText(getStatusLabel(status));
        holder.statusText.getBackground().setTint(ContextCompat.getColor(holder.itemView.getContext(), getStatusColor(status)));
        holder.statusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

        String members = challenge.getMemberCount() == 1
                ? holder.itemView.getContext().getString(R.string.solo_challenge)
                : holder.itemView.getContext().getString(R.string.members_count, challenge.getMemberCount());
        holder.metaText.setText(challenge.getDurationDays() + " days · " + members);

        holder.itemView.setOnClickListener(v -> listener.onChallengeClick(challenge));
    }

    private int getStatusColor(ChallengeStatus status) {
        switch (status) {
            case ACTIVE:
                return R.color.status_active;
            case COMPLETED:
                return R.color.status_completed;
            case DRAFT:
            default:
                return R.color.status_draft;
        }
    }

    private String getStatusLabel(ChallengeStatus status) {
        switch (status) {
            case ACTIVE:
                return "Active";
            case COMPLETED:
                return "Completed";
            case DRAFT:
            default:
                return "Draft";
        }
    }

    @Override
    public int getItemCount() {
        return challenges.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView descriptionText;
        final TextView statusText;
        final TextView metaText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            statusText = itemView.findViewById(R.id.statusText);
            metaText = itemView.findViewById(R.id.metaText);
        }
    }
}
