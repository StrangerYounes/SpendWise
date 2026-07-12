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
                    Toast.makeText(requireContext(), R.string.focus_session_finished, Toast.LENGTH_SHORT).show();
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
        leaderboardAdapter.setOnProfileClickListener(userId -> {
            Bundle args = new Bundle();
            args.putString("userId", userId);
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.action_detail_to_profile, args);
        });
        leaderboardAdapter.setOnKickListener(entry -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Kick Member")
                    .setMessage("Are you sure you want to remove " + entry.getDisplayName() + " from this challenge?")
                    .setPositiveButton("Kick", (d, w) -> viewModel.kickMember(entry.getUserId()))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.tasksRecyclerView.setAdapter(taskAdapter);
        binding.leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.leaderboardRecyclerView.setAdapter(leaderboardAdapter);

        binding.startButton.setOnClickListener(v -> viewModel.startChallenge());
        binding.skipButton.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.skip_day_title)
                    .setMessage(R.string.skip_day_message)
                    .setPositiveButton(R.string.use_skip, (d, w) -> viewModel.useSkipDay())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        viewModel.getChallenge().observe(getViewLifecycleOwner(), challenge -> {
            renderChallenge(challenge);
            setupMenu(challenge);
        });
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
        viewModel.getChallengeDeleted().observe(getViewLifecycleOwner(), deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                androidx.navigation.Navigation.findNavController(requireView()).navigateUp();
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

        leaderboardAdapter.setOwner(isCreator);

        binding.completedBanner.setVisibility(status == ChallengeStatus.COMPLETED ? View.VISIBLE : View.GONE);
        binding.draftActionsLayout.setVisibility(status == ChallengeStatus.DRAFT ? View.VISIBLE : View.GONE);

        if (status == ChallengeStatus.ACTIVE && challenge.getMaxSkips() > 0) {
            binding.skipsLayout.setVisibility(View.VISIBLE);
            int used = challenge.getMemberSkips() != null ? challenge.getMemberSkips().getOrDefault(currentUserId, 0) : 0;
            binding.skipsText.setText(getString(R.string.skips_used_format, used, challenge.getMaxSkips()));
            binding.skipButton.setEnabled(used < challenge.getMaxSkips());
        } else {
            binding.skipsLayout.setVisibility(View.GONE);
        }

        updateTasks();
    }

    private void setupMenu(Challenge challenge) {
        if (challenge == null) return;
        com.google.android.material.appbar.MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar == null) return;

        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_challenge_detail);

        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        boolean isCreator = currentUserId.equals(challenge.getCreatedBy());
        ChallengeStatus status = challenge.getStatusEnum();
        boolean isArchived = challenge.getArchivedMemberIds() != null && challenge.getArchivedMemberIds().contains(currentUserId);

        toolbar.getMenu().findItem(R.id.action_edit).setVisible(isCreator && status != ChallengeStatus.COMPLETED);
        toolbar.getMenu().findItem(R.id.action_share).setVisible(status == ChallengeStatus.ACTIVE || status == ChallengeStatus.COMPLETED);
        toolbar.getMenu().findItem(R.id.action_delete).setVisible(isCreator);
        toolbar.getMenu().findItem(R.id.action_leave).setVisible(!isCreator);
        toolbar.getMenu().findItem(R.id.action_archive).setVisible(status == ChallengeStatus.COMPLETED && !isArchived);

        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                Bundle args = new Bundle();
                args.putString("challengeId", challenge.getId());
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.action_detail_to_edit, args);
                return true;
            } else if (id == R.id.action_share) {
                viewModel.shareChallenge();
                return true;
            } else if (id == R.id.action_delete) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_challenge)
                        .setMessage(R.string.delete_challenge_message)
                        .setPositiveButton(R.string.delete_challenge, (d, w) -> viewModel.deleteChallenge())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            } else if (id == R.id.action_leave) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.leave_challenge)
                        .setMessage(R.string.leave_challenge_message)
                        .setPositiveButton(R.string.leave_challenge, (d, w) -> viewModel.leaveChallenge())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            } else if (id == R.id.action_archive) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.archive_challenge)
                        .setMessage(R.string.archive_challenge_message)
                        .setPositiveButton(R.string.archive_challenge, (d, w) -> viewModel.archiveChallenge())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            }
            return false;
        });
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
