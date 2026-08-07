package com.megaiptv.eltv;

import android.os.Bundle;

import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.media3.common.util.UnstableApi;

@UnstableApi
public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, new SettingsFragment(), android.R.id.content);
        }
    }
}

