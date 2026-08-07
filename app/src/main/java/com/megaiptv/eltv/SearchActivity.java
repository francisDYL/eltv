package com.megaiptv.eltv;

import android.os.Bundle;

import androidx.media3.common.util.UnstableApi;

@UnstableApi
public class SearchActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.search_fragment, new SearchFragment())
                    .commit();
        }
    }
}
