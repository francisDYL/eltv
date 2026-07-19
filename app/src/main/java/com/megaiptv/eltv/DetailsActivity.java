package com.megaiptv.eltv;

import android.os.Bundle;

public class DetailsActivity extends BaseActivity {

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