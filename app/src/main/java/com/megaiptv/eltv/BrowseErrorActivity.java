package com.megaiptv.eltv;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class BrowseErrorActivity extends FragmentActivity {

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