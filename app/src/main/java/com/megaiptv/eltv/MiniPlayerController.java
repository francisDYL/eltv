package com.megaiptv.eltv;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

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
public class MiniPlayerController {

    private final Activity    activity;
    private final View        container;
    private final PlayerView  playerView;
    private final TextView    channelName;

    public MiniPlayerController(Activity activity, View overlayRoot) {
        this.activity    = activity;
        this.container   = overlayRoot.findViewById(R.id.mini_player_container);
        this.playerView  = overlayRoot.findViewById(R.id.mini_player_view);
        this.channelName = overlayRoot.findViewById(R.id.mini_channel_name);

        // Bouton plein écran
        overlayRoot.findViewById(R.id.mini_expand_btn).setOnClickListener(v -> expand());

        // Bouton stop
        overlayRoot.findViewById(R.id.mini_stop_btn).setOnClickListener(v -> stop());
    }

    // ─── API publique ─────────────────────────────────────────────────────────

    /**
     * Met à jour la visibilité et le contenu du mini-player.
     * À appeler dans onResume() de BaseActivity.
     */
    public void update() {
        ExoPlayer player = PlayerManager.getInstance().getPlayerIfExists();
        if (player != null && PlayerManager.getInstance().isStreamActive()) {
            container.setVisibility(View.VISIBLE);
            playerView.setPlayer(player);
            String name = PlayerManager.getInstance().getCurrentChannelName();
            channelName.setText(name != null ? name : "");
        } else {
            container.setVisibility(View.GONE);
            playerView.setPlayer(null);
        }
    }

    /**
     * Détache le PlayerView du player sans stopper la lecture.
     * À appeler dans onPause() pour éviter les conflits quand
     * PlaybackActivity prend le premier plan.
     */
    public void detachView() {
        playerView.setPlayer(null);
    }

    /** Libère les références — appelé dans onDestroy(). */
    public void destroy() {
        playerView.setPlayer(null);
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

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
        container.setVisibility(View.GONE);
    }
}

