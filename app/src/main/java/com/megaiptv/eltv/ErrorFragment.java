package com.megaiptv.eltv;

import android.os.Bundle;

import androidx.leanback.app.ErrorSupportFragment;

public class ErrorFragment extends ErrorSupportFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.app_name));
        setMessage(getString(R.string.error_message));
        setDefaultBackground(false);
        setButtonText(getString(R.string.error_back));
        setButtonClickListener(v -> requireActivity().finish());
    }
}