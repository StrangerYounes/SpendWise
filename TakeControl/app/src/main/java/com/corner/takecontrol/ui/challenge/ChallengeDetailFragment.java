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

        taskAdapter = new ChallengeTaskAdapter(new ChallengeTaskAdapter.TaskActionListener() {
            @Override
            public void onComplete(ChallengeTask task, TaskProgress progress) {
                viewModel.saveProgress(progress);
            }

            @Override
            public void onLogProgress(ChallengeTask task, TaskProgress progress, double value) {
                viewModel.saveProgress(progress);
            }
        });

        leaderboardAdapter = new LeaderboardAdapter();

        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.tasksRecyclerView.setAdapter(taskAdapter);
        binding.leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.leaderboardRecyclerView.setAdapter(leaderboardAdapter);

        binding.startButton.setOnClickListener(v -> viewModel.startChallenge());
        binding.shareButton.setOnClickListener(v -> viewModel.shareChallenge());

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

        String challengeId = getArguments() != null ? getArguments().getString("challengeId") : null;
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
        binding.statusText.setText(challenge.getStatusEnum().name());

        if (challenge.getMemberCount() <= 1) {
            binding.membersText.setText(R.string.solo_challenge);
        } else {
            binding.membersText.setText(getString(R.string.members_count, challenge.getMemberCount()));
        }

        ChallengeStatus status = challenge.getStatusEnum();
        binding.completedBanner.setVisibility(status == ChallengeStatus.COMPLETED ? View.VISIBLE : View.GONE);
        binding.draftActionsLayout.setVisibility(status == ChallengeStatus.DRAFT ? View.VISIBLE : View.GONE);
        binding.socialActionsLayout.setVisibility(status == ChallengeStatus.ACTIVE || status == ChallengeStatus.COMPLETED
                ? View.VISIBLE : View.GONE);

        updateTasks();
    }

    private void updateTasks() {
        Challenge challenge = viewModel.getChallenge().getValue();
        List<ChallengeTask> tasks = viewModel.getTasks().getValue();
        List<TaskProgress> progress = viewModel.getProgressList().getValue();
        boolean readOnly = challenge == null || challenge.getStatusEnum() != ChallengeStatus.ACTIVE;
        taskAdapter.submitData(tasks, progress, readOnly);
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
