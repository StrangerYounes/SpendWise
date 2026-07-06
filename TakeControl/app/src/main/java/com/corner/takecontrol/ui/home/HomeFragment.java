package com.corner.takecontrol.ui.home;

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
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.databinding.FragmentHomeBinding;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private ChallengeAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        adapter = new ChallengeAdapter(this::openChallenge);
        binding.challengesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.challengesRecyclerView.setAdapter(adapter);

        binding.createFab.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_create));

        binding.joinFab.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_join));

        viewModel.getChallenges().observe(getViewLifecycleOwner(), this::renderChallenges);
        viewModel.getDisplayName().observe(getViewLifecycleOwner(), name -> {
            if (name != null) {
                adapter.setGreetingName(name);
            }
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.startListening();
    }

    private void renderChallenges(List<Challenge> challenges) {
        boolean empty = challenges == null || challenges.isEmpty();
        binding.emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.challengesRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        adapter.submitList(challenges);
    }

    private void openChallenge(Challenge challenge) {
        Bundle args = new Bundle();
        args.putString("challengeId", challenge.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_home_to_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
