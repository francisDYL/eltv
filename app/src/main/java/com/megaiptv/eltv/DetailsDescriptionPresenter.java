package com.megaiptv.eltv;

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        if (item instanceof Channel) {
            Channel ch = (Channel) item;
            viewHolder.getTitle().setText(ch.getName());
            viewHolder.getSubtitle().setText(ch.getCategory());
            viewHolder.getBody().setText(ch.getUrl());
        }
    }
}