package com.megaiptv.eltv;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.app.DetailsSupportFragmentBackgroundController;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.SparseArrayObjectAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoDetailsFragment extends DetailsSupportFragment {

    private static final int ACTION_PLAY      = 1;
    private static final int ACTION_FAVORITE  = 2;

    private Channel mChannel;
    private DetailsOverviewRow mDetailsRow;
    private SparseArrayObjectAdapter mActionsAdapter;
    private ArrayObjectAdapter mAdapter;
    private final DetailsSupportFragmentBackgroundController mBgController =
            new DetailsSupportFragmentBackgroundController(this);
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mChannel = (Channel) requireActivity().getIntent()
                .getSerializableExtra(DetailsActivity.CHANNEL, Channel.class);
        if (mChannel == null) { requireActivity().finish(); return; }

        mBgController.enableParallax();
        buildDetails();
    }

    private void buildDetails() {
        FullWidthDetailsOverviewRowPresenter rowPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        rowPresenter.setOnActionClickedListener(action -> {
            if (action.getId() == ACTION_PLAY) {
                Intent intent = new Intent(requireActivity(), PlaybackActivity.class);
                intent.putExtra(PlaybackActivity.CHANNEL_URL,  mChannel.getUrl());
                intent.putExtra(PlaybackActivity.CHANNEL_NAME, mChannel.getName());
                startActivity(intent);
            } else if (action.getId() == ACTION_FAVORITE) {
                toggleFavorite();
            }
        });

        FullWidthDetailsOverviewSharedElementHelper helper =
                new FullWidthDetailsOverviewSharedElementHelper();
        helper.setSharedElementEnterTransition(requireActivity(),
                DetailsActivity.CHANNEL, 500L);
        rowPresenter.setListener(helper);
        rowPresenter.setParticipatingEntranceTransition(false);

        ClassPresenterSelector selector = new ClassPresenterSelector();
        selector.addClassPresenter(DetailsOverviewRow.class, rowPresenter);

        mDetailsRow = new DetailsOverviewRow(mChannel);
        mActionsAdapter = new SparseArrayObjectAdapter();
        refreshActions();
        mDetailsRow.setActionsAdapter(mActionsAdapter);

        // Logo
        Glide.with(requireContext())
                .load(mChannel.getLogo())
                .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.default_channel_logo))
                .error(ContextCompat.getDrawable(requireContext(), R.drawable.default_channel_logo))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable r,
                                               @Nullable Transition<? super Drawable> t) {
                        mDetailsRow.setImageDrawable(r);
                    }
                    @Override public void onLoadCleared(@Nullable Drawable p) {}
                });

        mAdapter = new ArrayObjectAdapter(selector);
        mAdapter.add(mDetailsRow);
        setAdapter(mAdapter);
    }

    private void refreshActions() {
        mActionsAdapter.clear();
        mActionsAdapter.set(ACTION_PLAY,
                new Action(ACTION_PLAY, getString(R.string.play_channel)));
        mActionsAdapter.set(ACTION_FAVORITE,
                new Action(ACTION_FAVORITE, mChannel.isFavorite()
                        ? getString(R.string.remove_favorite)
                        : getString(R.string.add_favorite)));
    }

    private void toggleFavorite() {
        mChannel.setFavorite(!mChannel.isFavorite());
        refreshActions();
        mExecutor.execute(() ->
                AppDatabase.getDatabase(requireContext()).channelDao().update(mChannel));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }
}