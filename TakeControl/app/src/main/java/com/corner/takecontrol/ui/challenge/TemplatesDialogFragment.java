package com.corner.takecontrol.ui.challenge;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTemplate;
import com.corner.takecontrol.data.repository.TemplateRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class TemplatesDialogFragment extends DialogFragment {

    public interface OnTemplateSelectedListener {
        void onTemplateSelected(ChallengeTemplate template);
    }

    private OnTemplateSelectedListener listener;

    public void setOnTemplateSelectedListener(OnTemplateSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_templates, null);
        RecyclerView recyclerView = view.findViewById(R.id.templatesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        TemplateRepository repository = new TemplateRepository();
        List<ChallengeTemplate> templates = repository.getTemplates();
        
        recyclerView.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template, parent, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                ChallengeTemplate template = templates.get(position);
                holder.titleText.setText(template.getTitle());
                holder.descriptionText.setText(template.getDescription());
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTemplateSelected(template);
                    }
                    dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return templates.size();
            }
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.templates)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView descriptionText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.templateTitle);
            descriptionText = itemView.findViewById(R.id.templateDescription);
        }
    }
}
