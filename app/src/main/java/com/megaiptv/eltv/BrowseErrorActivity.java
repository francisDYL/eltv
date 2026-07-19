package com.megaiptv.eltv;

import android.os.Bundle;

public class BrowseErrorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.error_fragment, new ErrorFragment())
                    .commit();
        }
    }
}