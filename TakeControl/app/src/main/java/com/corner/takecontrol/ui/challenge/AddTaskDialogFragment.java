package com.corner.takecontrol.ui.challenge;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskFrequency;
import com.corner.takecontrol.data.model.TaskType;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddTaskDialogFragment extends DialogFragment {

    public interface TaskAddedListener {
        void onTaskAdded(ChallengeTask task);
    }

    private TaskAddedListener listener;

    public void setTaskAddedListener(TaskAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null);
        TextInputEditText titleInput = view.findViewById(R.id.taskTitleInput);
        Spinner frequencySpinner = view.findViewById(R.id.frequencySpinner);
        Spinner typeSpinner = view.findViewById(R.id.typeSpinner);
        TextInputLayout targetLayout = view.findViewById(R.id.targetLayout);
        TextInputEditText targetInput = view.findViewById(R.id.targetInput);
        TextInputLayout unitLayout = view.findViewById(R.id.unitLayout);
        TextInputEditText unitInput = view.findViewById(R.id.unitInput);

        ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.task_frequencies, android.R.layout.simple_spinner_item);
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(frequencyAdapter);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.task_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        typeSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                boolean checkmark = position == 0;
                targetLayout.setVisibility(checkmark ? View.GONE : View.VISIBLE);
                unitLayout.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }
        });

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_task)
                .setView(view)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(), "Task title required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    TaskFrequency frequency = TaskFrequency.values()[frequencySpinner.getSelectedItemPosition()];
                    TaskType type = TaskType.values()[typeSpinner.getSelectedItemPosition()];
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
                    if (listener != null) {
                        listener.onTaskAdded(task);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
