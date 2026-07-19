package com.corner.takecontrol.ui.challenge;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskFrequency;
import com.corner.takecontrol.data.model.TaskType;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.data.repository.UserRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddTaskDialogFragment extends DialogFragment {

    public interface TaskAddedListener {
        void onTaskAdded(ChallengeTask task);
    }

    public interface TaskUpdatedListener {
        void onTaskUpdated(int position, ChallengeTask task);
    }

    private TaskAddedListener listener;
    private TaskUpdatedListener updateListener;
    private ChallengeTask taskToEdit;
    private int editPosition = -1;
    private UserProfile currentUserProfile;

    public void setTaskAddedListener(TaskAddedListener listener) {
        this.listener = listener;
    }

    public void setTaskUpdatedListener(TaskUpdatedListener listener) {
        this.updateListener = listener;
    }

    public static AddTaskDialogFragment newInstance(ChallengeTask task, int position) {
        AddTaskDialogFragment fragment = new AddTaskDialogFragment();
        fragment.taskToEdit = task;
        fragment.editPosition = position;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null);
        TextInputEditText titleInput = view.findViewById(R.id.taskTitleInput);
        AutoCompleteTextView frequencySpinner = view.findViewById(R.id.frequencySpinner);
        AutoCompleteTextView typeSpinner = view.findViewById(R.id.typeSpinner);
        TextInputLayout targetLayout = view.findViewById(R.id.targetLayout);
        TextInputEditText targetInput = view.findViewById(R.id.targetInput);
        TextInputLayout unitLayout = view.findViewById(R.id.unitLayout);
        TextInputEditText unitInput = view.findViewById(R.id.unitInput);
        TextInputEditText timeInput = view.findViewById(R.id.timeInput);
        ChipGroup daysChipGroup = view.findViewById(R.id.daysChipGroup);
        
        MaterialButton advancedOptionsBtn = view.findViewById(R.id.advancedOptionsBtn);
        View advancedOptionsContainer = view.findViewById(R.id.advancedOptionsContainer);
        AutoCompleteTextView categoryInput = view.findViewById(R.id.categoryInput);
        AutoCompleteTextView actionInput = view.findViewById(R.id.actionInput);
        MaterialCheckBox optionalCheckbox = view.findViewById(R.id.optionalTaskCheckbox);
        MaterialCheckBox verifiableCheckbox = view.findViewById(R.id.verifiableTaskCheckbox);
        TextInputLayout verificationLayout = view.findViewById(R.id.verificationCodeLayout);
        TextInputEditText verificationInput = view.findViewById(R.id.verificationCodeInput);

        String[] frequencies = getResources().getStringArray(R.array.task_frequencies);
        String[] types = getResources().getStringArray(R.array.task_types);

        verifiableCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            verificationLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                typeSpinner.setText(types[0], false); // Force checkmark type
                typeSpinner.setEnabled(false);
                targetLayout.setVisibility(View.GONE);
                unitLayout.setVisibility(View.GONE);
            } else {
                typeSpinner.setEnabled(true);
            }
        });

        advancedOptionsBtn.setOnClickListener(v -> {
            boolean isVisible = advancedOptionsContainer.getVisibility() == View.VISIBLE;
            advancedOptionsContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            advancedOptionsBtn.setIconResource(isVisible ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
        });

        // Load suggestions from profile
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId != null) {
            new UserRepository().getUserProfile(userId, profile -> {
                if (profile != null && isAdded()) {
                    currentUserProfile = profile;
                    List<String> cats = new ArrayList<>(profile.getCustomCategories());
                    if (cats.isEmpty()) {
                        cats.addAll(Arrays.asList("Fitness", "Knowledge", "Health", "Productivity", "Self-Care", "Spiritual"));
                    }
                    categoryInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, cats));
                    
                    List<String> acts = new ArrayList<>(profile.getCustomActions());
                    if (!acts.isEmpty()) {
                        actionInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, acts));
                    }
                }
            });
        }

        ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, frequencies);
        frequencySpinner.setAdapter(frequencyAdapter);
        frequencySpinner.setText(frequencies[0], false);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, types);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setText(types[0], false);

        typeSpinner.setOnItemClickListener((parent, view1, pos, id) -> {
            boolean checkmark = pos == 0;
            targetLayout.setVisibility(checkmark ? View.GONE : View.VISIBLE);
            unitLayout.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
        });

        timeInput.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText("Select Task Time")
                    .build();
            picker.addOnPositiveButtonClickListener(v1 -> {
                timeInput.setText(String.format(java.util.Locale.US, "%02d:%02d", picker.getHour(), picker.getMinute()));
            });
            picker.show(getChildFragmentManager(), "timePicker");
        });

        if (taskToEdit != null) {
            titleInput.setText(taskToEdit.getTitle());
            int freqIdx = taskToEdit.getFrequencyEnum().ordinal();
            if (freqIdx >= 0 && freqIdx < frequencies.length) frequencySpinner.setText(frequencies[freqIdx], false);
            int typeIdx = taskToEdit.getTaskTypeEnum().ordinal();
            if (typeIdx >= 0 && typeIdx < types.length) typeSpinner.setText(types[typeIdx], false);
            targetInput.setText(String.valueOf(taskToEdit.getTargetValue()));
            unitInput.setText(taskToEdit.getUnit());
            timeInput.setText(taskToEdit.getExecutionTime());
            
            if (taskToEdit.getDaysOfWeek() != null) {
                int[] ids = {R.id.daySun, R.id.dayMon, R.id.dayTue, R.id.dayWed, R.id.dayThu, R.id.dayFri, R.id.daySat};
                for (int day : taskToEdit.getDaysOfWeek()) {
                    if (day >= 1 && day <= 7) daysChipGroup.check(ids[day - 1]);
                }
            }

            if (!TextUtils.isEmpty(taskToEdit.getManualCategory()) || !TextUtils.isEmpty(taskToEdit.getManualAction())
                    || taskToEdit.isOptional() || taskToEdit.isVerifiable()) {
                advancedOptionsContainer.setVisibility(View.VISIBLE);
                advancedOptionsBtn.setIconResource(R.drawable.ic_expand_less);
                categoryInput.setText(taskToEdit.getManualCategory());
                actionInput.setText(taskToEdit.getManualAction());
                optionalCheckbox.setChecked(taskToEdit.isOptional());
                verifiableCheckbox.setChecked(taskToEdit.isVerifiable());
                if (taskToEdit.isVerifiable()) {
                    verificationLayout.setVisibility(View.VISIBLE);
                    verificationInput.setText(taskToEdit.getVerificationCode());
                    typeSpinner.setEnabled(false);
                }
            }
            
            boolean checkmark = taskToEdit.getTaskTypeEnum() == TaskType.CHECKMARK;
            targetLayout.setVisibility(checkmark ? View.GONE : View.VISIBLE);
            unitLayout.setVisibility(taskToEdit.getTaskTypeEnum() == TaskType.NUMERIC ? View.VISIBLE : View.GONE);
        } else {
            targetLayout.setVisibility(View.GONE);
            unitLayout.setVisibility(View.GONE);
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(taskToEdit == null ? R.string.add_task : R.string.edit_task)
                .setView(view)
                .setPositiveButton(taskToEdit == null ? R.string.add : R.string.save, (dialog, which) -> {
                    String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(), "Task title required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int freqPos = -1;
                    String selectedFreq = frequencySpinner.getText().toString();
                    for (int i = 0; i < frequencies.length; i++) {
                        if (frequencies[i].equalsIgnoreCase(selectedFreq)) { freqPos = i; break; }
                    }

                    int typePos = -1;
                    String selectedType = typeSpinner.getText().toString();
                    for (int i = 0; i < types.length; i++) {
                        if (types[i].equalsIgnoreCase(selectedType)) { typePos = i; break; }
                    }

                    TaskFrequency frequency = TaskFrequency.values()[freqPos != -1 ? freqPos : 0];
                    TaskType type = TaskType.values()[typePos != -1 ? typePos : 0];
                    double target = 1;
                    String unit = "";
                    if (type != TaskType.CHECKMARK) {
                        String targetStr = targetInput.getText() != null ? targetInput.getText().toString().trim() : "";
                        if (TextUtils.isEmpty(targetStr)) {
                            Toast.makeText(requireContext(), "Target required", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        target = Double.parseDouble(targetStr);
                        unit = unitInput.getText() != null ? unitInput.getText().toString().trim() : "";
                        if (type == TaskType.DURATION) unit = "min";
                    }

                    ChallengeTask task = new ChallengeTask(title, frequency.getValue(), type.getValue(), target, unit, 0);
                    task.setExecutionTime(timeInput.getText().toString());
                    task.setManualCategory(categoryInput.getText().toString().trim());
                    task.setManualAction(actionInput.getText().toString().trim());
                    task.setOptional(optionalCheckbox.isChecked());
                    if (verifiableCheckbox.isChecked()) {
                        task.setVerificationCode(verificationInput.getText().toString().trim());
                        task.setTaskTypeEnum(TaskType.CHECKMARK);
                    }
                    
                    // Save new labels to profile if changed
                    if (currentUserProfile != null) {
                        boolean changed = false;
                        String mCat = task.getManualCategory();
                        if (!mCat.isEmpty() && !currentUserProfile.getCustomCategories().contains(mCat)) {
                            currentUserProfile.getCustomCategories().add(mCat);
                            changed = true;
                        }
                        String mAct = task.getManualAction();
                        if (!mAct.isEmpty() && !currentUserProfile.getCustomActions().contains(mAct)) {
                            currentUserProfile.getCustomActions().add(mAct);
                            changed = true;
                        }
                        if (changed) {
                            new UserRepository().updateUserProfile(currentUserProfile.getId(), currentUserProfile, null);
                        }
                    }

                    List<Integer> selectedDays = new ArrayList<>();
                    int[] ids = {R.id.daySun, R.id.dayMon, R.id.dayTue, R.id.dayWed, R.id.dayThu, R.id.dayFri, R.id.daySat};
                    for (int i = 0; i < ids.length; i++) {
                        if (daysChipGroup.getCheckedChipIds().contains(ids[i])) selectedDays.add(i + 1);
                    }
                    if (!selectedDays.isEmpty()) task.setDaysOfWeek(selectedDays);

                    if (taskToEdit == null) {
                        if (listener != null) listener.onTaskAdded(task);
                    } else {
                        if (updateListener != null) updateListener.onTaskUpdated(editPosition, task);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
