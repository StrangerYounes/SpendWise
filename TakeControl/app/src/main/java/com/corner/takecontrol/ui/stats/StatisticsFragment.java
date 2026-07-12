package com.corner.takecontrol.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.corner.takecontrol.FeatureFlags;
import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.data.model.UserStats;
import com.corner.takecontrol.databinding.FragmentStatisticsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;
    private StatisticsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        viewModel.getStats().observe(getViewLifecycleOwner(), this::updateStats);
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateLockStates);
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        binding.weeklyGraph.setOnDayClickListener(this::showDaySummary);
        binding.heatmapView.setOnCellClickListener(this::showDaySummary);
    }

    private void showDaySummary(String date, int count) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Summary for " + date)
                .setMessage("You completed " + count + (count == 1 ? " task" : " tasks") + " on this day.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateStats(UserStats stats) {
        if (stats == null) {
            binding.totalTasksText.setText("0");
            binding.categoryContainer.removeAllViews();
            binding.weeklyGraph.setData(null);
            binding.heatmapView.setData(null);
            return;
        }

        binding.totalTasksText.setText(String.valueOf(stats.getTotalTasksCompleted()));
        binding.weeklyGraph.setData(stats.getDailyCompletions());
        binding.heatmapView.setData(stats.getDailyCompletions());

        // Update Category Stats
        binding.categoryContainer.removeAllViews();
        for (Map.Entry<String, Double> entry : stats.getCategoryTotals().entrySet()) {
            if (entry.getValue() > 0) {
                addStatRow(binding.categoryContainer, entry.getKey(), entry.getValue());
            }
        }
    }

    private void addStatRow(LinearLayout container, String label, Double value) {
        View row = getLayoutInflater().inflate(R.layout.item_stat_row, container, false);
        TextView labelTv = row.findViewById(R.id.statLabelText);
        TextView valueTv = row.findViewById(R.id.statValueText);

        // Capitalize first letter of label
        String formattedLabel = label.substring(0, 1).toUpperCase() + label.substring(1);
        labelTv.setText(formattedLabel);
        
        // Format value: if it's a whole number, don't show .0
        String valStr;
        if (value == value.intValue()) {
            valStr = String.valueOf(value.intValue());
        } else {
            valStr = String.format(java.util.Locale.US, "%.1f", value);
        }
        
        valueTv.setText(valStr);

        container.addView(row);
    }

    private void updateLockStates(UserProfile profile) {
        int level = profile != null ? profile.getLevel() : 1;

        // Weekly Graph Lock
        updateSectionLock(level, FeatureFlags.LVL_THRESHOLD_GRAPHS, 
                binding.weeklyGraph, binding.weeklyLockedState.getRoot(), binding.weeklyLockedState.lockedMessageText);

        // Heatmap Lock
        updateSectionLock(level, FeatureFlags.LVL_THRESHOLD_HEATMAP,
                binding.heatmapView, binding.heatmapLockedState.getRoot(), binding.heatmapLockedState.lockedMessageText);

        // Category Stats Lock
        updateSectionLock(level, FeatureFlags.LVL_THRESHOLD_CATEGORIES,
                binding.categoryContainer, binding.categoryLockedState.getRoot(), binding.categoryLockedState.lockedMessageText);
    }

    private void updateSectionLock(int userLevel, int threshold, View content, View lockState, TextView lockMsg) {
        if (userLevel < threshold) {
            content.setVisibility(View.GONE);
            lockState.setVisibility(View.VISIBLE);
            lockMsg.setText("Unlock at Level " + threshold);
        } else {
            content.setVisibility(View.VISIBLE);
            lockState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
