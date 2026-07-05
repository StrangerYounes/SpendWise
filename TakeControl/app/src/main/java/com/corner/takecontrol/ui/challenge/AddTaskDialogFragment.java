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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
        AutoCompleteTextView frequencySpinner = view.findViewById(R.id.frequencySpinner);
        AutoCompleteTextView typeSpinner = view.findViewById(R.id.typeSpinner);
        TextInputLayout targetLayout = view.findViewById(R.id.targetLayout);
        TextInputEditText targetInput = view.findViewById(R.id.targetInput);
        TextInputLayout unitLayout = view.findViewById(R.id.unitLayout);
        TextInputEditText unitInput = view.findViewById(R.id.unitInput);

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

        // Default visibility
        targetLayout.setVisibility(View.GONE);
        unitLayout.setVisibility(View.GONE);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_task)
                .setView(view)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(), "Task title required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int freqPos = -1;
                    String selectedFreq = frequencySpinner.getText().toString();
                    for (int i = 0; i < frequencies.length; i++) {
                        if (frequencies[i].equals(selectedFreq)) {
                            freqPos = i;
                            break;
                        }
                    }

                    int typePos = -1;
                    String selectedType = typeSpinner.getText().toString();
                    for (int i = 0; i < types.length; i++) {
                        if (types[i].equals(selectedType)) {
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
                    if (listener != null) {
                        listener.onTaskAdded(task);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
