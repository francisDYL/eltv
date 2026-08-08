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
            AppDatabase db = null;
            boolean usingFallback = false;
            
            try {
                // Try to initialize database with retry logic
                int retries = 3;
                Exception lastError = null;
                while (retries > 0 && db == null) {
                    try {
                        db = AppDatabase.getDatabase(appCtx);
                    } catch (Exception e) {
                        lastError = e;
                        retries--;
                        if (retries > 0) {
                            Thread.sleep(500); // Wait before retry
                        }
                    }
                }
                
                if (db == null) {
                    // Activate in-memory fallback
                    usingFallback = true;
                    InMemoryChannelStore.getInstance().activate();
                    android.util.Log.w("SettingsFragment", "Database unavailable, using in-memory storage");
                }
                
                Source src = new Source(finalUrl, "Source M3U");
                
                if (usingFallback) {
                    InMemoryChannelStore store = InMemoryChannelStore.getInstance();
                    store.deleteChannelsBySource(finalUrl);
                } else {
                    db.sourceDao().insert(src);
                    db.channelDao().deleteBySource(finalUrl);
                }
                
                String content = NetworkUtils.fetchUrl(finalUrl);
                if (content == null || content.trim().isEmpty()) {
                    throw new Exception("Empty response from URL");
                }
                
                List<Channel> channels = M3UParser.parseM3U(content, finalUrl);
                if (channels == null || channels.isEmpty()) {
                    throw new Exception("No channels found in playlist");
                }
                
                if (usingFallback) {
                    InMemoryChannelStore store = InMemoryChannelStore.getInstance();
                    store.insertChannels(channels);
                    src.setLastSync(System.currentTimeMillis());
                    store.insertSource(src);
                } else {
                    db.channelDao().insertAll(channels);
                    src.setLastSync(System.currentTimeMillis());
                    db.sourceDao().insert(src);
                }
                
                int count = channels.size();
                final boolean finalUsingFallback = usingFallback;
                handler.post(() -> {
                    String message = count + " " + appCtx.getString(R.string.sync_success);
                    if (finalUsingFallback) {
                        message += "\n⚠️ Database failed - using temporary storage (data won't persist)";
                    }
                    Toast.makeText(appCtx, message, Toast.LENGTH_LONG).show();
                    // Go back to refresh the main screen
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("SettingsFragment", "Sync failed", e);
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

