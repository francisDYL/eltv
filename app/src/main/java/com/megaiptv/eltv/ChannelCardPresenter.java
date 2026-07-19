package com.megaiptv.eltv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;

public class ChannelCardPresenter extends Presenter {

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View card = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel_card, parent, false);
        return new CardViewHolder(card);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, Object item) {
        Channel channel = (Channel) item;
        CardViewHolder holder = (CardViewHolder) viewHolder;

        holder.name.setText(channel.getName());
        holder.group.setText(channel.getGroup() != null ? channel.getGroup() : "");

        // Glide utilise ELTVGlideModule → OkHttpClient trust-all + timeout 30s
        // Même pattern que les apps IPTV de référence : .into(imageView) direct
        Glide.with(holder.logo)
                .load(channel.getLogo())
                .fitCenter()
                .placeholder(R.drawable.default_channel_logo)
                .error(R.drawable.default_channel_logo)
                .into(holder.logo);
    }

    @Override
    public void onUnbindViewHolder(@NonNull ViewHolder viewHolder) {
        CardViewHolder holder = (CardViewHolder) viewHolder;
        Glide.with(holder.logo).clear(holder.logo);
    }
}
