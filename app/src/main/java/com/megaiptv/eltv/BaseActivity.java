package com.megaiptv.eltv;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;

/**
 * Activité de base pour toutes les activités de l'application SAUF PlaybackActivity.
 *
 * Gère l'overlay mini-player global :
 *  - Injecte le layout mini_player_overlay dans android.R.id.content
 *  - Attache / détache le PlayerView selon le cycle de vie
 *  - Garantit qu'un seul PlayerView est attaché à l'ExoPlayer à la fois
 */
public abstract class BaseActivity extends FragmentActivity {

    private MiniPlayerController miniPlayer;
    private boolean              overlayAttached = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * onStart est appelé après que le contenu de l'activité est visible.
     * On injecte le mini-player ici pour qu'il s'applique quel que soit
     * le chemin de création (setContentView ou GuidedStep.addAsRoot).
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (!overlayAttached) {
            attachMiniPlayerOverlay();
        }
    }

    /** Met à jour le mini-player quand l'activité reprend le premier plan. */
    @Override
    protected void onResume() {
        super.onResume();
        if (miniPlayer != null) miniPlayer.update();
    }

    /**
     * Détache le PlayerView du mini-player avant de passer en arrière-plan.
     * Permet à PlaybackActivity de s'attacher librement sans conflit.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayer != null) miniPlayer.detachView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (miniPlayer != null) {
            miniPlayer.destroy();
            miniPlayer = null;
        }
    }

    // ─── Injection de l'overlay ───────────────────────────────────────────────

    private void attachMiniPlayerOverlay() {
        try {
            ViewGroup contentRoot = getWindow().getDecorView()
                    .getRootView().findViewById(android.R.id.content);
            if (contentRoot == null) return;

            android.view.View overlay = getLayoutInflater()
                    .inflate(R.layout.mini_player_overlay, contentRoot, false);
            contentRoot.addView(overlay);

            miniPlayer      = new MiniPlayerController(this, overlay);
            overlayAttached = true;
        } catch (Exception ignored) {
            // Ne pas faire planter l'activité si l'overlay échoue
        }
    }
}

