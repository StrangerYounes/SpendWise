package com.corner.takecontrol.ui.auth;

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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.corner.takecontrol.R;
import com.corner.takecontrol.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        if (viewModel.isSignedIn()) {
            NavController navController = Navigation.findNavController(view);
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() == R.id.loginFragment) {
                navController.navigate(R.id.action_login_to_home);
            }
            return;
        }

        binding.signInButton.setOnClickListener(v -> {
            String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
            String password = binding.passwordInput.getText() != null ? binding.passwordInput.getText().toString() : "";
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "Email and password required", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.signIn(email, password);
        });

        binding.goToRegisterButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_register));

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        viewModel.getAuthSuccess().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                NavController navController = Navigation.findNavController(view);
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() == R.id.loginFragment) {
                    navController.navigate(R.id.action_login_to_home);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
