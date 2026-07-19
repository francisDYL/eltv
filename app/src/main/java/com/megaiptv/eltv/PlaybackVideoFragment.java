package com.megaiptv.eltv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.ui.PlayerView;

public class PlaybackVideoFragment extends Fragment {

    private PlayerView mPlayerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPlayerView = view.findViewById(R.id.player_view);

        String url  = requireActivity().getIntent().getStringExtra(PlaybackActivity.CHANNEL_URL);
        String name = requireActivity().getIntent().getStringExtra(PlaybackActivity.CHANNEL_NAME);
        if (url != null && !url.isEmpty()) {
            PlayerManager.getInstance().play(requireContext(), url, name != null ? name : "");
            mPlayerView.setPlayer(PlayerManager.getInstance().getPlayer(requireContext()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPlayerView != null)
            mPlayerView.setPlayer(PlayerManager.getInstance().getPlayer(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPlayerView != null) mPlayerView.setPlayer(null);
    }
}