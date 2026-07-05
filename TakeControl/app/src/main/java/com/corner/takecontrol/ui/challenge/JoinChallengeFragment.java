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

import com.corner.takecontrol.R;
import com.corner.takecontrol.databinding.FragmentJoinChallengeBinding;

public class JoinChallengeFragment extends Fragment {

    private FragmentJoinChallengeBinding binding;
    private JoinChallengeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentJoinChallengeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JoinChallengeViewModel.class);

        binding.joinButton.setOnClickListener(v -> {
            String code = binding.shareCodeInput.getText() != null
                    ? binding.shareCodeInput.getText().toString() : "";
            viewModel.joinChallenge(code);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getJoinedChallengeId().observe(getViewLifecycleOwner(), challengeId -> {
            if (challengeId != null) {
                Bundle args = new Bundle();
                args.putString("challengeId", challengeId);
                Navigation.findNavController(view).navigate(R.id.action_join_to_detail, args);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
