package com.megaiptv.eltv;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

/**
 * Activité d'affichage des détails d'une chaîne.
 *
 * Mini-player docké en haut à droite, intégré dans activity_details.xml.
 *
 * Navigation D-PAD :
 *   - UP depuis les boutons d'action (Play / Favoris) : si le focus ne peut
 *     plus monter dans Leanback, il saute au bouton ⏸/▶ du mini-player.
 *   - DOWN depuis le mini-player : retour aux détails (géré par BaseActivity).
 */
public class DetailsActivity extends BaseActivity {

    public static final String CHANNEL = "channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        setupMiniPlayer();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_fragment, new VideoDetailsFragment())
                    .commit();
        }
    }

    @Override
    protected View getContentFragmentContainer() {
        return findViewById(R.id.details_fragment);
    }

    /**
     * Étend la gestion D-PAD de BaseActivity :
     * DPAD_UP depuis le contenu Leanback → mini-player play/pause (si visible).
     * Si le focus ne peut plus monter (sommet du contenu), on saute au mini-player.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {

            View container = findViewById(R.id.mini_player_container);
            boolean miniVisible = container != null
                    && container.getVisibility() == View.VISIBLE;

            if (miniVisible && !container.hasFocus()) {
                View before = getCurrentFocus();
                boolean handled = super.dispatchKeyEvent(event);
                View after = getCurrentFocus();
                // Si le focus n'a pas bougé → on est au sommet → sauter au mini-player
                if (after == before || after == null) {
                    View btn = findViewById(R.id.mini_play_pause_btn);
                    if (btn != null) {
                        btn.requestFocus();
                        return true;
                    }
                }
                return handled;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}