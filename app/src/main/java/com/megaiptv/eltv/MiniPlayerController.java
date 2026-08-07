package com.megaiptv.eltv;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/**
 * Contrôle le mini-player overlay présent dans toutes les activités
 * sauf PlaybackActivity (qui est la vue plein écran).
 *
 * Stratégie de partage du player :
 *   - Un seul ExoPlayer singleton (PlayerManager)
 *   - Quand PlaybackActivity est au premier plan → PlayerView full screen s'attache au player
 *     (Media3 détache automatiquement le mini-player)
 *   - Quand l'utilisateur revient → BaseActivity.onResume() réattache le mini-player
 */
@UnstableApi
public class MiniPlayerController {

    private final Activity   activity;
    private final View       container;
    private final PlayerView playerView;
    private final TextView   channelName;
    private final TextView   playPauseBtn;

    public MiniPlayerController(Activity activity, View overlayRoot) {
        this.activity     = activity;
        this.container    = overlayRoot.findViewById(R.id.mini_player_container);
        this.playerView   = overlayRoot.findViewById(R.id.mini_player_view);
        this.channelName  = overlayRoot.findViewById(R.id.mini_channel_name);
        this.playPauseBtn = overlayRoot.findViewById(R.id.mini_play_pause_btn);

        // Bouton play / pause
        if (playPauseBtn != null) {
            playPauseBtn.setOnClickListener(v -> togglePlayPause());
        }

        // Bouton plein écran
        View expandBtn = overlayRoot.findViewById(R.id.mini_expand_btn);
        if (expandBtn != null) expandBtn.setOnClickListener(v -> expand());

        // Bouton stop
        View stopBtn = overlayRoot.findViewById(R.id.mini_stop_btn);
        if (stopBtn != null) stopBtn.setOnClickListener(v -> stop());
    }

    // ─── API publique ─────────────────────────────────────────────────────────

    /**
     * Met à jour la visibilité et le contenu du mini-player.
     * À appeler dans onResume() de BaseActivity.
     */
    public void update() {
        PlayerManager pm = PlayerManager.getInstance();
        ExoPlayer player = pm.getPlayerIfExists();
        if (container != null && player != null && (pm.isStreamActive() || pm.hasStream())) {
            container.setVisibility(View.VISIBLE);
            if (playerView != null) playerView.setPlayer(player);
            String name = pm.getCurrentChannelName();
            if (channelName != null) channelName.setText(name != null ? name : "");
            refreshPlayPauseIcon();
        } else if (container != null) {
            container.setVisibility(View.GONE);
            if (playerView != null) playerView.setPlayer(null);
        }
    }

    /**
     * Détache le PlayerView du player sans stopper la lecture.
     * À appeler dans onPause() pour éviter les conflits quand
     * PlaybackActivity prend le premier plan.
     */
    public void detachView() {
        if (playerView != null) {
            playerView.setPlayer(null);
        }
    }

    /** Libère les références — appelé dans onDestroy(). */
    public void destroy() {
        if (playerView != null) {
            playerView.setPlayer(null);
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private void togglePlayPause() {
        PlayerManager pm = PlayerManager.getInstance();
        if (pm.isPlaying()) {
            pm.pause();
        } else {
            pm.resume();
        }
        refreshPlayPauseIcon();
    }

    private void refreshPlayPauseIcon() {
        if (playPauseBtn == null) return;
        playPauseBtn.setText(PlayerManager.getInstance().isPlaying() ? "⏸" : "▶");
    }

    private void expand() {
        String url  = PlayerManager.getInstance().getCurrentUrl();
        String name = PlayerManager.getInstance().getCurrentChannelName();
        if (url != null && !url.isEmpty()) {
            Intent intent = new Intent(activity, PlaybackActivity.class);
            intent.putExtra(PlaybackActivity.CHANNEL_URL,  url);
            intent.putExtra(PlaybackActivity.CHANNEL_NAME, name != null ? name : "");
            activity.startActivity(intent);
        }
    }

    private void stop() {
        PlayerManager.getInstance().release();
        if (container != null) {
            container.setVisibility(View.GONE);
        }
    }
}
