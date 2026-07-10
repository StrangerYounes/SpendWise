package com.corner.takecontrol.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.databinding.ItemCosmeticRewardBinding;
import com.corner.takecontrol.util.ProgressionUtil;

import java.util.ArrayList;
import java.util.List;

public class CosmeticRewardAdapter extends RecyclerView.Adapter<CosmeticRewardAdapter.ViewHolder> {

    private final List<ProgressionUtil.CosmeticItem> items = new ArrayList<>();
    private final List<String> unlockedIds = new ArrayList<>();
    private String equippedId;
    private final OnEquipClickListener listener;

    public interface OnEquipClickListener {
        void onEquip(String id);
    }

    public CosmeticRewardAdapter(OnEquipClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<ProgressionUtil.CosmeticItem> newItems, List<String> newUnlocked, String newEquipped) {
        items.clear();
        items.addAll(newItems);
        unlockedIds.clear();
        if (newUnlocked != null) unlockedIds.addAll(newUnlocked);
        equippedId = newEquipped;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCosmeticRewardBinding binding = ItemCosmeticRewardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProgressionUtil.CosmeticItem item = items.get(position);
        holder.bind(item, unlockedIds.contains(item.id), item.id.equals(equippedId));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCosmeticRewardBinding binding;

        ViewHolder(ItemCosmeticRewardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProgressionUtil.CosmeticItem item, boolean isUnlocked, boolean isEquipped) {
            binding.rewardName.setText(item.name);
            
            boolean effectivelyUnlocked = isUnlocked || item.id.equals("NONE");
            
            if (effectivelyUnlocked) {
                binding.rewardRequirement.setText(R.string.unlocked);
                binding.equipButton.setVisibility(isEquipped ? View.GONE : View.VISIBLE);
                binding.rewardStatus.setVisibility(isEquipped ? View.VISIBLE : View.GONE);
                binding.rewardStatus.setText(R.string.equipped);
            } else {
                binding.rewardRequirement.setText(binding.getRoot().getContext().getString(R.string.locked_reward, item.requiredLevel));
                binding.equipButton.setVisibility(View.GONE);
                binding.rewardStatus.setVisibility(View.GONE);
            }

            binding.equipButton.setOnClickListener(v -> listener.onEquip(item.id));
        }
    }
}
