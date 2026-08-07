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
        String savedUrl = requireContext()
                .getSharedPreferences("eltv_prefs", Context.MODE_PRIVATE)
                .getString("last_m3u_url", "");
        
        int theme = ThemeManager.getInstance().getCurrentTheme();

        // URL input
        actions.add(new GuidedAction.Builder(requireContext())
                .id(ACTION_URL)
                .title(getString(R.string.add_source))
                .editTitle(savedUrl)
                .description(savedUrl.isEmpty() ? getString(R.string.add_source_desc) : savedUrl)
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
        if (action.getId() == ACTION_URL) {
            CharSequence editTitle = action.getEditTitle();
            String url = (editTitle != null) ? editTitle.toString().trim() : "";
            
            // Update description to show the current URL
            action.setDescription(url.isEmpty() ? getString(R.string.add_source_desc) : url);
            
            // Persist the URL
            requireContext().getSharedPreferences("eltv_prefs", Context.MODE_PRIVATE)
                    .edit().putString("last_m3u_url", url).apply();
            
            // Notify the adapter that the action has changed to update the description view
            notifyActionChanged(findActionPositionById(ACTION_URL));
        }
        return GuidedAction.ACTION_ID_NEXT;
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        long id = action.getId();
        if (id == ACTION_SYNC) {
            // Before syncing, ensure we capture any pending edit
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
        
        String tempUrl = "";
        if (urlAction.getEditTitle() != null) {
            tempUrl = urlAction.getEditTitle().toString().trim();
        }
        
        if (tempUrl.isEmpty()) {
            tempUrl = requireContext().getSharedPreferences("eltv_prefs", Context.MODE_PRIVATE)
                    .getString("last_m3u_url", "").trim();
        }

        if (tempUrl.isEmpty()) {
            toast(R.string.error_empty_url);
            return;
        }

        // Basic validation: ensure it starts with a protocol
        if (!tempUrl.toLowerCase().startsWith("http") && !tempUrl.toLowerCase().startsWith("rtsp")) {
            Toast.makeText(requireContext(), "URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalUrl = tempUrl;
        toast(R.string.sync_in_progress);

        Context appCtx = requireContext().getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(appCtx);
                Source src = new Source(finalUrl, "Source M3U");
                db.sourceDao().insert(src);
                db.channelDao().deleteBySource(finalUrl);
                String content = NetworkUtils.fetchUrl(finalUrl);
                List<Channel> channels = M3UParser.parseM3U(content, finalUrl);
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

