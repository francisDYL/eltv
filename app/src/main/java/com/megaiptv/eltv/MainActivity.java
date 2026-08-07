package com.megaiptv.eltv;

import android.os.Bundle;
import android.view.View;

import androidx.core.splashscreen.SplashScreen;
import androidx.media3.common.util.UnstableApi;

@UnstableApi
public class MainActivity extends BaseActivity implements ThemeManager.ThemeChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialisation du thème AVANT super.onCreate pour éviter les conflits avec Splash Screen
        ThemeManager.getInstance().init(this);
        SplashScreen.installSplashScreen(this);
        
        super.onCreate(savedInstanceState);
        ThemeManager.getInstance().addListener(this);
        setContentView(R.layout.activity_main);

        // Mini-player intégré dans activity_main.xml (pas d'overlay flottant)
        setupMiniPlayer();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new MainFragment())
                    .commit();
        }
    }

    /** Permet à dispatchKeyEvent de BaseActivity de retourner le focus vers le contenu. */
    @Override
    protected View getContentFragmentContainer() {
        return findViewById(R.id.main_browse_fragment);
    }

    @Override
    public void onThemeChanged(int theme) {
        // MainFragment se met à jour via son propre listener
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThemeManager.getInstance().removeListener(this);
    }
}