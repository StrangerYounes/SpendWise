package com.corner.takecontrol.ui.challenge;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;
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
        }, (position, task) -> {
            AddTaskDialogFragment dialog = AddTaskDialogFragment.newInstance(task, position);
            dialog.setTaskUpdatedListener((pos, updatedTask) -> {
                viewModel.updatePendingTask(pos, updatedTask);
            });
            dialog.show(getChildFragmentManager(), "EditTask");
        });

        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.tasksRecyclerView.setAdapter(taskAdapter);

        setupReordering();
        setupDurationChips();

        binding.addTaskButton.setOnClickListener(v -> {
            AddTaskDialogFragment dialog = new AddTaskDialogFragment();
            dialog.setTaskAddedListener(task -> {
                viewModel.addPendingTask(task);
            });
            dialog.show(getChildFragmentManager(), "AddTask");
        });

        binding.saveChallengeButton.setOnClickListener(v -> {
            String title = binding.titleInput.getText() != null ? binding.titleInput.getText().toString().trim() : "";
            String description = binding.descriptionInput.getText() != null ? binding.descriptionInput.getText().toString().trim() : "";
            int duration = getSelectedDuration();
            
            int maxSkips = 0;
            String skipsStr = binding.maxSkipsInput.getText() != null ? binding.maxSkipsInput.getText().toString() : "";
            if (!skipsStr.isEmpty()) {
                try {
                    maxSkips = Integer.parseInt(skipsStr);
                } catch (NumberFormatException ignored) {}
            }

            viewModel.createChallenge(title, description, duration, maxSkips);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.saveChallengeButton.setEnabled(!loading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getChallengeCreated().observe(getViewLifecycleOwner(), challengeId -> {
            if (challengeId != null) {
                viewModel.clear();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });

        viewModel.getChallengeToEdit().observe(getViewLifecycleOwner(), challenge -> {
            if (challenge != null) {
                renderChallenge(challenge);
            }
        });

        viewModel.getTasksUpdated().observe(getViewLifecycleOwner(), tasks -> {
            taskAdapter.submitList(tasks);
        });

        String challengeId = getArguments() != null ? getArguments().getString("challengeId") : null;
        if (challengeId != null) {
            binding.saveChallengeButton.setText(R.string.update_challenge);
            viewModel.loadChallenge(challengeId);
        }

        String templateId = getArguments() != null ? getArguments().getString("templateId") : null;
        if (templateId != null) {
            viewModel.loadTemplate(templateId);
        }
    }

    private void renderChallenge(Challenge challenge) {
        binding.titleInput.setText(challenge.getTitle());
        binding.descriptionInput.setText(challenge.getDescription());
        binding.maxSkipsInput.setText(String.valueOf(challenge.getMaxSkips()));
        
        int duration = challenge.getDurationDays();
        if (duration == 7) binding.chip7.setChecked(true);
        else if (duration == 30) binding.chip30.setChecked(true);
        else if (duration == 90) binding.chip90.setChecked(true);
        else {
            binding.chipCustom.setChecked(true);
            binding.customDurationLayout.setVisibility(View.VISIBLE);
            binding.customDurationInput.setText(String.valueOf(duration));
        }
        selectedDuration = duration;
    }

    private void setupDurationChips() {
        binding.durationChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            binding.customDurationLayout.setVisibility(checkedId == R.id.chipCustom ? View.VISIBLE : View.GONE);

            if (checkedId == R.id.chip7) selectedDuration = 7;
            else if (checkedId == R.id.chip30) selectedDuration = 30;
            else if (checkedId == R.id.chip90) selectedDuration = 90;
        });
    }

    private void setupReordering() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();
                viewModel.moveTask(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not implemented
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.tasksRecyclerView);
    }

    private int getSelectedDuration() {
        if (binding.chipCustom.isChecked()) {
            String custom = binding.customDurationInput.getText() != null ? binding.customDurationInput.getText().toString() : "";
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
