package com.corner.takecontrol.ui.challenge;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.corner.takecontrol.R;
import com.corner.takecontrol.databinding.FragmentCreateChallengeBinding;
import com.google.android.material.chip.Chip;

public class CreateChallengeFragment extends Fragment {

    private FragmentCreateChallengeBinding binding;
    private CreateChallengeViewModel viewModel;
    private PendingTaskAdapter taskAdapter;
    private int selectedDuration = 7;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateChallengeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CreateChallengeViewModel.class);

        taskAdapter = new PendingTaskAdapter(position -> {
            viewModel.removePendingTask(position);
            taskAdapter.submitList(viewModel.getPendingTasks());
        });
        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.tasksRecyclerView.setAdapter(taskAdapter);

        setupDurationChips();

        binding.addTaskButton.setOnClickListener(v -> {
            AddTaskDialogFragment dialog = new AddTaskDialogFragment();
            dialog.setTaskAddedListener(task -> {
                viewModel.addPendingTask(task);
                taskAdapter.submitList(viewModel.getPendingTasks());
            });
            dialog.show(getParentFragmentManager(), "add_task");
        });

        binding.saveChallengeButton.setOnClickListener(v -> {
            String title = binding.titleInput.getText() != null ? binding.titleInput.getText().toString() : "";
            String description = binding.descriptionInput.getText() != null
                    ? binding.descriptionInput.getText().toString() : "";
            viewModel.createChallenge(title, description, getSelectedDuration());
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getChallengeCreated().observe(getViewLifecycleOwner(), challengeId -> {
            if (challengeId != null) {
                Bundle args = new Bundle();
                args.putString("challengeId", challengeId);
                Navigation.findNavController(view).navigate(R.id.action_create_to_detail, args);
                viewModel.clear();
            }
        });
    }

    private void setupDurationChips() {
        binding.chip7.setOnClickListener(v -> selectDuration(7, binding.chip7));
        binding.chip30.setOnClickListener(v -> selectDuration(30, binding.chip30));
        binding.chip90.setOnClickListener(v -> selectDuration(90, binding.chip90));
        binding.chipCustom.setOnClickListener(v -> {
            selectDuration(-1, binding.chipCustom);
            binding.customDurationLayout.setVisibility(View.VISIBLE);
        });
    }

    private void selectDuration(int days, Chip selectedChip) {
        for (Chip chip : new Chip[]{binding.chip7, binding.chip30, binding.chip90, binding.chipCustom}) {
            chip.setChecked(chip == selectedChip);
        }
        selectedDuration = days;
        if (days != -1) {
            binding.customDurationLayout.setVisibility(View.GONE);
        }
    }

    private int getSelectedDuration() {
        if (selectedDuration == -1) {
            String custom = binding.customDurationInput.getText() != null
                    ? binding.customDurationInput.getText().toString().trim() : "";
            try {
                return Integer.parseInt(custom);
            } catch (NumberFormatException e) {
                return 7;
            }
        }
        return selectedDuration;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
