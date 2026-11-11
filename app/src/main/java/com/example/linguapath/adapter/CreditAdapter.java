package com.example.linguapath.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.databinding.ItemCreditBinding;
import com.example.linguapath.model.Credit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CreditAdapter extends RecyclerView.Adapter<CreditAdapter.CreditViewHolder> {

    private final List<Credit> items = new ArrayList<>();
    private final Consumer<String> linkListener;

    public CreditAdapter(Consumer<String> linkListener) {
        this.linkListener = linkListener;
    }

    public void setItems(List<Credit> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CreditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCreditBinding binding = ItemCreditBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CreditViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CreditViewHolder holder, int position) {
        Credit credit = items.get(position);
        ItemCreditBinding b = holder.binding;

        b.textCreditTitle.setText(credit.title);
        b.textCreditBody.setText(credit.body);

        // Rows get recycled: set the visibility every time
        if (credit.url == null) {
            b.buttonCreditLink.setVisibility(View.GONE);
        } else {
            b.buttonCreditLink.setVisibility(View.VISIBLE);
            b.buttonCreditLink.setOnClickListener(v -> linkListener.accept(credit.url));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CreditViewHolder extends RecyclerView.ViewHolder {
        final ItemCreditBinding binding;

        CreditViewHolder(ItemCreditBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}