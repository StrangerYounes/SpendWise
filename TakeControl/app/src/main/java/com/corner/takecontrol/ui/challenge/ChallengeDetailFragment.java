package com.corner.takecontrol.ui.challenge;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeStatus;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskProgress;
import com.corner.takecontrol.databinding.FragmentChallengeDetailBinding;
import com.corner.takecontrol.ui.challenge.FocusTimerDialogFragment;
import com.corner.takecontrol.util.ReminderManager;
import com.corner.takecontrol.util.ChallengeUiUtil;

import java.util.List;

public class ChallengeDetailFragment extends Fragment {

    private FragmentChallengeDetailBinding binding;
    private ChallengeDetailViewModel viewModel;
    private ChallengeTaskAdapter taskAdapter;
    private LeaderboardAdapter leaderboardAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChallengeDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChallengeDetailViewModel.class);

        String challengeId = getArguments() != null ? getArguments().getString("challengeId") : null;

        taskAdapter = new ChallengeTaskAdapter(new ChallengeTaskAdapter.TaskActionListener() {
            @Override
            public void onComplete(ChallengeTask task, TaskProgress progress) {
                viewModel.saveProgress(progress);
            }

            @Override
            public void onLogProgress(ChallengeTask task, TaskProgress progress, double value) {
                viewModel.saveProgress(progress);
            }

            @Override
            public void onResetProgress(ChallengeTask task, TaskProgress progress) {
                viewModel.saveProgress(progress);
            }

            @Override
            public void onStartTimer(ChallengeTask task, TaskProgress progress) {
                FocusTimerDialogFragment dialog = FocusTimerDialogFragment.newInstance(task, progress);
                dialog.setOnTimerFinishedListener((t, p, minutes) -> {
                    double currentVal = p.getValue();
                    double newValue = currentVal + minutes;
                    p.setValue(newValue);
                    p.setCompleted(newValue >= t.getTargetValue());
                    viewModel.saveProgress(p);
                    Toast.makeText(requireContext(), "Focus session finished!", Toast.LENGTH_SHORT).show();
                });
                dialog.show(getChildFragmentManager(), "FocusTimer");
            }
        });

        leaderboardAdapter = new LeaderboardAdapter(
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                        ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : "",
                entry -> {
                    viewModel.nudgeMember(entry.getUserId(), entry.getDisplayName());
                    Toast.makeText(requireContext(), "Nudge sent!", Toast.LENGTH_SHORT).show();
                });

        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.tasksRecyclerView.setAdapter(taskAdapter);
        binding.leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.leaderboardRecyclerView.setAdapter(leaderboardAdapter);

        binding.startButton.setOnClickListener(v -> viewModel.startChallenge());
        binding.shareButton.setOnClickListener(v -> viewModel.shareChallenge());
        binding.skipButton.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Use Skip Day?")
                    .setMessage("This will mark all tasks for today as skipped. You won't break your streak.")
                    .setPositiveButton("Use Skip", (d, w) -> viewModel.useSkipDay())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        binding.editButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("challengeId", challengeId);
            androidx.navigation.Navigation.findNavController(v).navigate(R.id.action_detail_to_edit, args);
        });

        viewModel.getChallenge().observe(getViewLifecycleOwner(), this::renderChallenge);
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> updateTasks());
        viewModel.getProgressList().observe(getViewLifecycleOwner(), progress -> updateTasks());
        viewModel.getCompletionPercent().observe(getViewLifecycleOwner(), percent -> {
            if (percent != null) {
                binding.progressText.setText(getString(R.string.progress_percent, percent));
                binding.progressBar.setProgress(percent);
            }
        });
        viewModel.getLeaderboard().observe(getViewLifecycleOwner(), this::renderLeaderboard);
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getShareCode().observe(getViewLifecycleOwner(), code -> {
            if (code != null) {
                copyShareCode(code);
            }
        });

        if (challengeId != null) {
            viewModel.load(challengeId);
        }
    }

    private void renderChallenge(Challenge challenge) {
        if (challenge == null) {
            return;
        }

        binding.challengeTitleText.setText(challenge.getTitle());
        binding.challengeDescriptionText.setText(challenge.getDescription() != null ? challenge.getDescription() : "");
        ChallengeStatus status = challenge.getStatusEnum();
        binding.statusText.setText(getString(ChallengeUiUtil.getStatusLabelRes(status)));
        binding.statusText.getBackground().setTint(requireContext().getColor(ChallengeUiUtil.getStatusColorRes(status)));
        binding.statusText.setTextColor(requireContext().getColor(R.color.white));

        ChallengeUiUtil.bindDaysRemaining(requireContext(), binding.daysRemainingText, challenge);

        if (challenge.getMemberCount() <= 1) {
            binding.membersText.setText(R.string.solo_challenge);
        } else {
            binding.membersText.setText(getString(R.string.members_count, challenge.getMemberCount()));
        }

        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        boolean isCreator = currentUserId.equals(challenge.getCreatedBy());

        binding.completedBanner.setVisibility(status == ChallengeStatus.COMPLETED ? View.VISIBLE : View.GONE);
        binding.draftActionsLayout.setVisibility(status == ChallengeStatus.DRAFT ? View.VISIBLE : View.GONE);
        binding.socialActionsLayout.setVisibility(status == ChallengeStatus.ACTIVE || status == ChallengeStatus.COMPLETED
                ? View.VISIBLE : View.GONE);
        binding.editButton.setVisibility(isCreator && status != ChallengeStatus.COMPLETED ? View.VISIBLE : View.GONE);

        if (status == ChallengeStatus.ACTIVE && challenge.getMaxSkips() > 0) {
            binding.skipsLayout.setVisibility(View.VISIBLE);
            int used = challenge.getMemberSkips() != null ? challenge.getMemberSkips().getOrDefault(currentUserId, 0) : 0;
            binding.skipsText.setText(String.format(java.util.Locale.US, "Skips used: %d / %d", used, challenge.getMaxSkips()));
            binding.skipButton.setEnabled(used < challenge.getMaxSkips());
        } else {
            binding.skipsLayout.setVisibility(View.GONE);
        }

        updateTasks();
    }

    private void updateTasks() {
        Challenge challenge = viewModel.getChallenge().getValue();
        List<ChallengeTask> tasks = viewModel.getTasks().getValue();
        List<TaskProgress> progress = viewModel.getProgressList().getValue();
        boolean readOnly = challenge == null || challenge.getStatusEnum() != ChallengeStatus.ACTIVE;
        taskAdapter.submitData(tasks, progress, readOnly);

        if (!readOnly && tasks != null) {
            ReminderManager.scheduleReminders(requireContext(), tasks);
        }
    }

    private void renderLeaderboard(List<com.corner.takecontrol.data.model.LeaderboardEntry> entries) {
        boolean show = entries != null && !entries.isEmpty();
        binding.leaderboardTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.leaderboardRecyclerView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            leaderboardAdapter.submitList(entries);
        }
    }

    private void copyShareCode(String code) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("share_code", code));
        }
        Toast.makeText(requireContext(), getString(R.string.share_code_copied, code), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
