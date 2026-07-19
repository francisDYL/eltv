package com.megaiptv.eltv;

import android.os.Bundle;
import android.view.View;

/**
 * Activité d'affichage des détails d'une chaîne.
 *
 * Mini-player docké en haut à droite, intégré dans activity_details.xml.
 * Accessible via D-PAD : presser UP depuis le contenu pour y accéder,
 * DOWN depuis le mini-player pour revenir aux détails.
 */
public class DetailsActivity extends BaseActivity {

    public static final String CHANNEL = "channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Mini-player intégré dans activity_details.xml (pas d'overlay flottant)
        setupMiniPlayer();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_fragment, new VideoDetailsFragment())
                    .commit();
        }
    }

    /** Permet à dispatchKeyEvent de BaseActivity de retourner le focus vers les détails. */
    @Override
    protected View getContentFragmentContainer() {
        return findViewById(R.id.details_fragment);
    }
}