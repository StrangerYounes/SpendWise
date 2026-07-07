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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
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

        String[] frequencies = getResources().getStringArray(R.array.task_frequencies);
        ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, frequencies);
        frequencySpinner.setAdapter(frequencyAdapter);
        frequencySpinner.setText(frequencies[0], false);

        String[] types = getResources().getStringArray(R.array.task_types);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, types);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setText(types[0], false);

        typeSpinner.setOnItemClickListener((parent, view1, position, id) -> {
            boolean checkmark = position == 0;
            targetLayout.setVisibility(checkmark ? View.GONE : View.VISIBLE);
            unitLayout.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
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
            
            // Use localized strings from the arrays
            int freqIdx = taskToEdit.getFrequencyEnum().ordinal();
            if (freqIdx >= 0 && freqIdx < frequencies.length) {
                frequencySpinner.setText(frequencies[freqIdx], false);
            }
            
            int typeIdx = taskToEdit.getTaskTypeEnum().ordinal();
            if (typeIdx >= 0 && typeIdx < types.length) {
                typeSpinner.setText(types[typeIdx], false);
            }

            targetInput.setText(String.valueOf(taskToEdit.getTargetValue()));
            unitInput.setText(taskToEdit.getUnit());
            timeInput.setText(taskToEdit.getExecutionTime());
            
            if (taskToEdit.getDaysOfWeek() != null) {
                int[] ids = {R.id.daySun, R.id.dayMon, R.id.dayTue, R.id.dayWed, R.id.dayThu, R.id.dayFri, R.id.daySat};
                for (int day : taskToEdit.getDaysOfWeek()) {
                    if (day >= 1 && day <= 7) {
                        daysChipGroup.check(ids[day - 1]);
                    }
                }
            }
            
            boolean checkmark = taskToEdit.getTaskTypeEnum() == TaskType.CHECKMARK;
            targetLayout.setVisibility(checkmark ? View.GONE : View.VISIBLE);
            unitLayout.setVisibility(taskToEdit.getTaskTypeEnum() == TaskType.NUMERIC ? View.VISIBLE : View.GONE);
        } else {
            // Default visibility
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
                        if (frequencies[i].equalsIgnoreCase(selectedFreq)) {
                            freqPos = i;
                            break;
                        }
                    }

                    int typePos = -1;
                    String selectedType = typeSpinner.getText().toString();
                    for (int i = 0; i < types.length; i++) {
                        if (types[i].equalsIgnoreCase(selectedType)) {
                            typePos = i;
                            break;
                        }
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
                        if (type == TaskType.DURATION) {
                            unit = "min";
                        }
                    }

                    ChallengeTask task = new ChallengeTask(title, frequency.getValue(), type.getValue(), target, unit, 0);
                    task.setExecutionTime(timeInput.getText().toString());
                    
                    List<Integer> selectedDays = new ArrayList<>();
                    int[] ids = {R.id.daySun, R.id.dayMon, R.id.dayTue, R.id.dayWed, R.id.dayThu, R.id.dayFri, R.id.daySat};
                    for (int i = 0; i < ids.length; i++) {
                        if (daysChipGroup.getCheckedChipIds().contains(ids[i])) {
                            selectedDays.add(i + 1);
                        }
                    }
                    if (!selectedDays.isEmpty()) {
                        task.setDaysOfWeek(selectedDays);
                    }

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
