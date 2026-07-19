package com.megaiptv.eltv;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class DetailsActivity extends FragmentActivity {

    public static final String CHANNEL = "channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_fragment, new VideoDetailsFragment())
                    .commit();
        }
    }

}