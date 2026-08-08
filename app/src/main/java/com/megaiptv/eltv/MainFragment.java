package com.megaiptv.eltv;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainFragment extends BrowseSupportFragment implements ThemeManager.ThemeChangeListener {

    private static final int BACKGROUND_DELAY = 300;

    private final Handler          mHandler   = new Handler(Looper.getMainLooper());
    private final ExecutorService  mExecutor  = Executors.newSingleThreadExecutor();
    private BackgroundManager      mBgManager;
    private Drawable               mDefaultBg;
    private DisplayMetrics         mMetrics;
    private Timer                  mBgTimer;
    private String                 mBgUri;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Defer background manager attachment to ensure window is ready on 4K hardware
        // Use post to ensure window decorations are fully initialized
        mHandler.post(() -> {
            if (mBgManager == null && getActivity() != null) {
                prepareBackground();
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        applyTheme();
        ThemeManager.getInstance().addListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadChannels();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBgTimer != null) mBgTimer.cancel();
        ThemeManager.getInstance().removeListener(this);
        mExecutor.shutdown();
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    @Override
    public void onThemeChanged(int theme) {
        applyTheme();
    }

    private void applyTheme() {
        try {
            ThemeManager tm = ThemeManager.getInstance();
            setBrandColor(tm.getBrandColor());
            setSearchAffordanceColor(tm.getSearchAffordanceColor());
        } catch (Exception ignored) {}
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void prepareBackground() {
        try {
            android.app.Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) return;
            
            // Get metrics first to determine if we should enable background
            mMetrics = activity.getResources().getDisplayMetrics();
            
            // For 4K displays (width >= 3840), use a more conservative approach
            boolean is4K = mMetrics.widthPixels >= 3840 || mMetrics.heightPixels >= 2160;
            
            if (is4K) {
                // On 4K TVs, skip dynamic backgrounds to avoid OOM crashes
                android.util.Log.i("MainFragment", "4K display detected, using static background");
                // Just set the default background color instead of BackgroundManager
                if (activity.getWindow() != null) {
                    activity.getWindow().setBackgroundDrawableResource(R.drawable.default_background);
                }
                mDefaultBg = ContextCompat.getDrawable(activity, R.drawable.default_background);
                return;
            }
            
            // For non-4K displays, use BackgroundManager as before
            mBgManager = BackgroundManager.getInstance(activity);
            if (!mBgManager.isAttached()) {
                mBgManager.attach(activity.getWindow());
            }
            mDefaultBg = ContextCompat.getDrawable(activity, R.drawable.default_background);
        } catch (Exception e) {
            android.util.Log.e("MainFragment", "BackgroundManager setup failed", e);
            // Fallback: set static background
            try {
                android.app.Activity activity = getActivity();
                if (activity != null && activity.getWindow() != null) {
                    activity.getWindow().setBackgroundDrawableResource(R.drawable.default_background);
                }
            } catch (Exception ignored) {}
        }
    }

    private void setupUI() {
        setTitle(getString(R.string.app_name));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
    }

    private void setupListeners() {
        setOnSearchClickedListener(v -> startActivity(new Intent(requireActivity(), SearchActivity.class)));
        setOnItemViewClickedListener(new ItemClickListener());
        setOnItemViewSelectedListener(new ItemSelectListener());
    }

    // ─── Data loading ─────────────────────────────────────────────────────────

    private void loadChannels() {
        // Extraire les chaînes de caractères sur le thread principal
        final String defaultUrl      = getString(R.string.default_m3u_url);
        final String favLabel        = getString(R.string.favorites);
        final String settingsLabel   = getString(R.string.settings);
        final String sourcesLabel    = getString(R.string.settings_sources);
        final String themeLabel      = getString(R.string.settings_theme);

        mExecutor.execute(() -> {
            final Map<String, List<Channel>> rowsMap = new LinkedHashMap<>();
            
            try {
                android.content.Context context = getContext();
                if (context == null) {
                    // Context lost, show settings only
                    mHandler.post(() -> buildRows(rowsMap, settingsLabel, sourcesLabel, themeLabel));
                    return;
                }
                
                AppDatabase db = AppDatabase.getDatabase(context);

                // Premier démarrage : ajoute la source par défaut et synchronise
                List<Source> sources = db.sourceDao().getAll();
                if (sources.isEmpty() && !defaultUrl.isEmpty()) {
                    try {
                        Source src = new Source(defaultUrl, "IPTV-ORG");
                        db.sourceDao().insert(src);
                        String content = NetworkUtils.fetchUrl(defaultUrl);
                        List<Channel> channels = M3UParser.parseM3U(content, defaultUrl);
                        db.channelDao().insertAll(channels);
                        src.setLastSync(System.currentTimeMillis());
                        db.sourceDao().insert(src);
                    } catch (Exception e) {
                        android.util.Log.w("MainFragment", "Failed to load default source", e);
                    }
                }

                List<Channel> favorites = db.channelDao().getFavorites();
                List<String>  groups    = db.channelDao().getGroups();

                if (!favorites.isEmpty()) rowsMap.put(favLabel, favorites);
                
                for (String g : groups) {
                    if (g == null || g.isEmpty()) continue;
                    List<Channel> ch = db.channelDao().getByGroup(g);
                    if (!ch.isEmpty()) rowsMap.put(g, ch);
                }
            } catch (Exception e) {
                android.util.Log.e("MainFragment", "Error loading channels", e);
            } finally {
                // Always show settings, even if channel loading failed
                mHandler.post(() -> {
                    if (getContext() != null) {
                        buildRows(rowsMap, settingsLabel, sourcesLabel, themeLabel);
                    }
                });
            }
        });
    }

    private void buildRows(Map<String, List<Channel>> rowsMap, String settingsLabel, 
                          String sourcesLabel, String themeLabel) {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        int i = 0;

        // Show welcome message if no channels
        if (rowsMap.isEmpty()) {
            ArrayObjectAdapter welcomeRow = new ArrayObjectAdapter(new WelcomePresenter());
            welcomeRow.add("Welcome to ELTV!");
            rowsAdapter.add(new ListRow(new HeaderItem(i++, "📺 Getting Started"), welcomeRow));
        }

        // Add channel rows
        for (Map.Entry<String, List<Channel>> e : rowsMap.entrySet()) {
            ArrayObjectAdapter row = new ArrayObjectAdapter(new ChannelCardPresenter());
            row.addAll(0, e.getValue());
            rowsAdapter.add(new ListRow(new HeaderItem(i++, e.getKey()), row));
        }

        // Add settings row
        ArrayObjectAdapter settingsRow = new ArrayObjectAdapter(new SettingsTilePresenter());
        settingsRow.add(sourcesLabel);
        settingsRow.add(themeLabel);
        rowsAdapter.add(new ListRow(new HeaderItem(i, settingsLabel), settingsRow));

        setAdapter(rowsAdapter);
    }
    
    private void showEmptyState() {
        // No longer needed - welcome state is shown in buildRows
    }

    // ─── Background ───────────────────────────────────────────────────────────

    private void updateBackground(String uri) {
        if (uri == null || uri.isEmpty()) { 
            if (mBgManager != null) mBgManager.setDrawable(mDefaultBg); 
            return; 
        }

        android.app.Activity activity = getActivity();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) return;

        // Cap background resolution to 1080p even on 4K to avoid OOM
        int width  = Math.min(mMetrics.widthPixels,  1920);
        int height = Math.min(mMetrics.heightPixels, 1080);

        Glide.with(activity)
                .load(uri)
                .centerCrop()
                .error(mDefaultBg)
                .into(new CustomTarget<Drawable>(width, height) {
                    @Override
                    public void onResourceReady(@NonNull Drawable r,
                                               @Nullable Transition<? super Drawable> t) {
                        if (mBgManager != null) mBgManager.setDrawable(r);
                    }
                    @Override public void onLoadCleared(@Nullable Drawable p) {}
                });
        if (mBgTimer != null) mBgTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (mBgTimer != null) mBgTimer.cancel();
        mBgTimer = new Timer();
        mBgTimer.schedule(new TimerTask() {
            @Override public void run() { mHandler.post(() -> updateBackground(mBgUri)); }
        }, BACKGROUND_DELAY);
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private final class ItemClickListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder ivh, Object item,
                                  RowPresenter.ViewHolder rvh, Row row) {
            if (item instanceof Channel) {
                Intent intent = new Intent(requireActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.CHANNEL, (Channel) item);
                startActivity(intent);
            } else if (item instanceof String) {
                Intent intent = new Intent(requireActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        }
    }

    private final class ItemSelectListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder ivh, Object item,
                                   RowPresenter.ViewHolder rvh, Row row) {
            if (item instanceof Channel) {
                mBgUri = ((Channel) item).getLogo();
                startBackgroundTimer();
            }
        }
    }

    // ─── Settings tile presenter ──────────────────────────────────────────────

    private class SettingsTilePresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(220, 220));
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(true);
            tv.setBackgroundColor(ThemeManager.getInstance().getBrandColor());
            tv.setTextColor(0xFFFFFFFF);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(14f);
            tv.setPadding(16, 16, 16, 16);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override public void onUnbindViewHolder(ViewHolder viewHolder) {}
    }
    
    // ─── Welcome presenter ────────────────────────────────────────────────────

    private class WelcomePresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(800, 300));
            tv.setFocusable(false);
            tv.setBackgroundColor(0x40FFFFFF);
            tv.setTextColor(0xFFFFFFFF);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(16f);
            tv.setPadding(32, 32, 32, 32);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            TextView tv = (TextView) viewHolder.view;
            tv.setText("📺 Welcome to ELTV!\n\n" +
                    "No channels found. Get started by adding an M3U playlist source.\n\n" +
                    "Navigate to Settings → Sources M3U below to add your playlist.");
        }

        @Override public void onUnbindViewHolder(ViewHolder viewHolder) {}
    }

}