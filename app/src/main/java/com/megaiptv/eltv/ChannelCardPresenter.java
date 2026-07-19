package com.megaiptv.eltv;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class ChannelCardPresenter extends Presenter {

    private static final int CARD_WIDTH  = 320;
    private static final int CARD_HEIGHT = 180;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context ctx = parent.getContext();
        ImageCardView card = new ImageCardView(ctx);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setBackgroundColor(ContextCompat.getColor(ctx, R.color.card_background));
        return new ViewHolder(card);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Channel channel = (Channel) item;
        ImageCardView card = (ImageCardView) viewHolder.view;

        card.setTitleText(channel.getName());
        card.setContentText(channel.getGroup());
        card.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

        String logo = channel.getLogo();
        if (logo != null && !logo.isEmpty()) {
            Glide.with(card.getContext())
                    .load(logo)
                    .centerCrop()
                    .placeholder(ContextCompat.getDrawable(card.getContext(), R.drawable.default_channel_logo))
                    .error(ContextCompat.getDrawable(card.getContext(), R.drawable.default_channel_logo))
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource,
                                                    @Nullable Transition<? super Drawable> transition) {
                            card.setMainImage(resource);
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        } else {
            card.setMainImage(ContextCompat.getDrawable(card.getContext(), R.drawable.default_channel_logo));
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageCardView card = (ImageCardView) viewHolder.view;
        card.setMainImage(null);
    }
}

