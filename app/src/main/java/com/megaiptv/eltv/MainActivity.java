package com.megaiptv.eltv;

import android.os.Bundle;
import android.view.View;

import androidx.core.splashscreen.SplashScreen;
import androidx.media3.common.util.UnstableApi;

@UnstableApi
public class MainActivity extends BaseActivity implements ThemeManager.ThemeChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            SplashScreen.installSplashScreen(this);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "SplashScreen failed (non-critical)", e);
        }
        super.onCreate(savedInstanceState);
        
        try {
            ThemeManager.getInstance().init(this);
            ThemeManager.getInstance().addListener(this);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "ThemeManager init failed", e);
        }
        
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