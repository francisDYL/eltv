package com.megaiptv.eltv;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class PlaybackActivity extends FragmentActivity {

    public static final String CHANNEL_URL  = "channel_url";
    public static final String CHANNEL_NAME = "channel_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.playback_container, new PlaybackVideoFragment())
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            PlayerManager.getInstance().release();
        }
    }
}