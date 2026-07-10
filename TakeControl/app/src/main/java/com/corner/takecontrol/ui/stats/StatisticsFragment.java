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
    }

    private void updateStats(UserStats stats) {
        if (stats == null) {
            binding.totalTasksText.setText("0");
            binding.volumeContainer.removeAllViews();
            binding.categoryContainer.removeAllViews();
            return;
        }

        binding.totalTasksText.setText(String.valueOf(stats.getTotalTasksCompleted()));

        // Update Volume Stats
        binding.volumeContainer.removeAllViews();
        for (Map.Entry<String, Double> entry : stats.getUnitTotals().entrySet()) {
            if (entry.getValue() > 0) {
                addStatRow(binding.volumeContainer, entry.getKey(), entry.getValue());
            }
        }

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
        if (value == value.intValue()) {
            valueTv.setText(String.valueOf(value.intValue()));
        } else {
            valueTv.setText(String.format(java.util.Locale.US, "%.1f", value));
        }

        container.addView(row);
    }

    private void updateLockStates(UserProfile profile) {
        int level = profile != null ? profile.getLevel() : 1;

        // Volume Stats Lock
        if (level < FeatureFlags.LVL_THRESHOLD_VOLUME) {
            binding.volumeContainer.setVisibility(View.GONE);
            binding.volumeLockedState.getRoot().setVisibility(View.VISIBLE);
            binding.volumeLockedState.lockedMessageText.setText("Unlock at Level " + FeatureFlags.LVL_THRESHOLD_VOLUME);
        } else {
            binding.volumeContainer.setVisibility(View.VISIBLE);
            binding.volumeLockedState.getRoot().setVisibility(View.GONE);
        }

        // Category Stats Lock
        if (level < FeatureFlags.LVL_THRESHOLD_CATEGORIES) {
            binding.categoryContainer.setVisibility(View.GONE);
            binding.categoryLockedState.getRoot().setVisibility(View.VISIBLE);
            binding.categoryLockedState.lockedMessageText.setText("Unlock at Level " + FeatureFlags.LVL_THRESHOLD_CATEGORIES);
        } else {
            binding.categoryContainer.setVisibility(View.VISIBLE);
            binding.categoryLockedState.getRoot().setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
