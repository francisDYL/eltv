package com.megaiptv.eltv;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class MainActivity extends FragmentActivity implements ThemeManager.ThemeChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.getInstance().init(this);
        ThemeManager.getInstance().addListener(this);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new MainFragment())
                    .commit();
        }
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