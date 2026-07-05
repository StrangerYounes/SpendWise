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
import androidx.navigation.Navigation;

import com.corner.takecontrol.R;
import com.corner.takecontrol.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.registerButton.setOnClickListener(v -> {
            String name = binding.displayNameInput.getText() != null
                    ? binding.displayNameInput.getText().toString().trim() : "";
            String email = binding.emailInput.getText() != null
                    ? binding.emailInput.getText().toString().trim() : "";
            String password = binding.passwordInput.getText() != null
                    ? binding.passwordInput.getText().toString() : "";

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.register(email, password, name);
        });

        binding.goToLoginButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_register_to_login));

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
                Navigation.findNavController(view).navigate(R.id.action_register_to_home);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
