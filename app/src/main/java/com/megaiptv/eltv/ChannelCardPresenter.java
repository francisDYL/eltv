package com.megaiptv.eltv;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class ChannelCardPresenter extends Presenter {

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static final class CardViewHolder extends ViewHolder {
        final ImageView logo;
        final TextView  name;
        final TextView  group;

        CardViewHolder(View view) {
            super(view);
            logo  = view.findViewById(R.id.channel_logo);
            name  = view.findViewById(R.id.channel_name);
            group = view.findViewById(R.id.channel_group);
        }
    }

    // ─── Presenter ────────────────────────────────────────────────────────────

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View card = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel_card, parent, false);
        return new CardViewHolder(card);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Channel channel = (Channel) item;
        CardViewHolder holder = (CardViewHolder) viewHolder;

        holder.name.setText(channel.getName());
        holder.group.setText(channel.getGroup() != null ? channel.getGroup() : "");

        String logoUrl = channel.getLogo();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(holder.logo.getContext())
                    .load(logoUrl)
                    .centerCrop()
                    .placeholder(ContextCompat.getDrawable(
                            holder.logo.getContext(), R.drawable.default_channel_logo))
                    .error(ContextCompat.getDrawable(
                            holder.logo.getContext(), R.drawable.default_channel_logo))
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable r,
                                                   @Nullable Transition<? super Drawable> t) {
                            holder.logo.setImageDrawable(r);
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable p) {}
                    });
        } else {
            holder.logo.setImageDrawable(
                    ContextCompat.getDrawable(holder.logo.getContext(),
                            R.drawable.default_channel_logo));
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        CardViewHolder holder = (CardViewHolder) viewHolder;
        Glide.with(holder.logo.getContext()).clear(holder.logo);
        holder.logo.setImageDrawable(null);
    }
}
