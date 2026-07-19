package com.megaiptv.eltv;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import java.util.List;
import java.util.concurrent.Executors;

public class SettingsFragment extends GuidedStepSupportFragment {

    private static final long ACTION_URL      = 1;
    private static final long ACTION_SYNC     = 2;
    private static final long ACTION_MIDNIGHT = 3;
    private static final long ACTION_FOREST   = 4;
    private static final long ACTION_PURPLE   = 5;

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(
                getString(R.string.settings),
                getString(R.string.settings_description),
                getString(R.string.app_name),
                null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        String defaultUrl = getString(R.string.default_m3u_url);
        int theme = ThemeManager.getInstance().getCurrentTheme();

        // URL input
        actions.add(new GuidedAction.Builder(requireContext())
                .id(ACTION_URL)
                .title(defaultUrl)
                .editTitle(defaultUrl)
                .description(getString(R.string.add_source_desc))
                .editable(true)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI)
                .build());

        // Sync button
        actions.add(new GuidedAction.Builder(requireContext())
                .id(ACTION_SYNC)
                .title(getString(R.string.sync_all))
                .description(getString(R.string.sync_all_desc))
                .build());

        // Theme selection
        actions.add(new GuidedAction.Builder(requireContext())
                .id(ACTION_MIDNIGHT)
                .title("🌙 Midnight")
                .description("Bleu nuit — cinéma")
                .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                .checked(theme == ThemeManager.THEME_MIDNIGHT)
                .build());

        actions.add(new GuidedAction.Builder(requireContext())
                .id(ACTION_FOREST)
                .title("🌲 Forest")
                .description("Vert nature")
                .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                .checked(theme == ThemeManager.THEME_FOREST)
                .build());

        actions.add(new GuidedAction.Builder(requireContext())
                .id(ACTION_PURPLE)
                .title("💜 Purple")
                .description("Violet moderne")
                .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                .checked(theme == ThemeManager.THEME_PURPLE)
                .build());
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        return GuidedAction.ACTION_ID_NEXT;
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        long id = action.getId();
        if (id == ACTION_SYNC) {
            doSync();
        } else if (id == ACTION_MIDNIGHT) {
            ThemeManager.getInstance().setTheme(requireContext(), ThemeManager.THEME_MIDNIGHT);
            toast(R.string.theme_applied);
        } else if (id == ACTION_FOREST) {
            ThemeManager.getInstance().setTheme(requireContext(), ThemeManager.THEME_FOREST);
            toast(R.string.theme_applied);
        } else if (id == ACTION_PURPLE) {
            ThemeManager.getInstance().setTheme(requireContext(), ThemeManager.THEME_PURPLE);
            toast(R.string.theme_applied);
        }
    }

    private void doSync() {
        GuidedAction urlAction = findActionById(ACTION_URL);
        if (urlAction == null) return;
        CharSequence seq = urlAction.getTitle();
        if (seq == null) return;
        String url = seq.toString().trim();
        if (url.isEmpty()) { toast(R.string.error_empty_url); return; }

        toast(R.string.sync_in_progress);
        Context appCtx = requireContext().getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(appCtx);
                Source src = new Source(url, "Source M3U");
                db.sourceDao().insert(src);
                db.channelDao().deleteBySource(url);
                String content = NetworkUtils.fetchUrl(url);
                List<Channel> channels = M3UParser.parseM3U(content, url);
                db.channelDao().insertAll(channels);
                src.setLastSync(System.currentTimeMillis());
                db.sourceDao().insert(src);
                int count = channels.size();
                handler.post(() -> Toast.makeText(appCtx,
                        count + " " + appCtx.getString(R.string.sync_success),
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(appCtx,
                        appCtx.getString(R.string.sync_error) + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void toast(int resId) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
    }
}

