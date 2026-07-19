package com.megaiptv.eltv;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

/**
 * Activité de base pour toutes les activités de l'application SAUF PlaybackActivity.
 *
 * Gestion du mini-player :
 *  - Le mini-player est INTÉGRÉ dans le layout de chaque activité fille (pas d'overlay flottant)
 *  - {@link #setupMiniPlayer()} initialise {@link MiniPlayerController} sur les vues du layout
 *  - {@link #dispatchKeyEvent} permet la navigation D-PAD vers/depuis le mini-player :
 *      · DPAD_UP  depuis le contenu → mini-player play/pause quand focus bloqué en haut
 *      · DPAD_DOWN depuis mini-player → retour au contenu
 *      · MEDIA_PLAY_PAUSE partout → bascule play/pause
 */
public abstract class BaseActivity extends FragmentActivity {

    /** Contrôleur du mini-player intégré dans le layout de l'activité. */
    protected MiniPlayerController miniPlayerController;

    // ─── Cycle de vie ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Initialise le {@link MiniPlayerController} en cherchant les vues
     * à partir de la DecorView (qui contient le layout de l'activité).
     * À appeler APRÈS {@code setContentView()} dans les sous-classes.
     */
    protected void setupMiniPlayer() {
        try {
            miniPlayerController = new MiniPlayerController(this, getWindow().getDecorView());
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniPlayerController != null) miniPlayerController.update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayerController != null) miniPlayerController.detachView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (miniPlayerController != null) {
            miniPlayerController.destroy();
            miniPlayerController = null;
        }
    }

    // ─── Navigation D-PAD vers le mini-player ────────────────────────────────

    /**
     * Retourne le conteneur du fragment principal (BrowseFragment, DetailsFragment…).
     * Surcharger dans chaque sous-classe pour permettre le retour du focus
     * depuis le mini-player vers le contenu principal.
     */
    protected View getContentFragmentContainer() {
        return null;
    }

    /**
     * Gestion des touches globales :
     * <ul>
     *   <li>MEDIA_PLAY_PAUSE / MEDIA_PLAY / MEDIA_PAUSE : bascule lecture/pause partout.</li>
     *   <li>DPAD_DOWN depuis le mini-player : retour au contenu principal.</li>
     * </ul>
     *
     * NOTE : La navigation DPAD_UP vers le mini-player est intentionnellement
     * absente ici. Elle doit être activée uniquement dans les activités où le
     * mini-player est accessible via D-PAD (ex : DetailsActivity).
     * Dans MainActivity la navigation sur les chaînes ne doit pas être interrompue.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }

        int key = event.getKeyCode();

        // ── Touche média play/pause (télécommande) ──────────────────────────
        if (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || key == KeyEvent.KEYCODE_MEDIA_PLAY
                || key == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            PlayerManager pm = PlayerManager.getInstance();
            if (pm.hasStream()) {
                if (pm.isPlaying()) pm.pause(); else pm.resume();
                if (miniPlayerController != null) miniPlayerController.update();
                return true;
            }
        }

        // ── DPAD_DOWN depuis le mini-player → retour au contenu ─────────────
        View container = findViewById(R.id.mini_player_container);
        boolean miniVisible = container != null
                && container.getVisibility() == View.VISIBLE;

        if (miniVisible && container.hasFocus()
                && key == KeyEvent.KEYCODE_DPAD_DOWN) {
            boolean handled = super.dispatchKeyEvent(event);
            if (container.hasFocus()) {
                View content = getContentFragmentContainer();
                if (content != null) {
                    content.requestFocus();
                    return true;
                }
            }
            return handled;
        }

        return super.dispatchKeyEvent(event);
    }
}
