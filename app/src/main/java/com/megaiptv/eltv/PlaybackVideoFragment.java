package com.megaiptv.eltv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;

@UnstableApi
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
        startPlaybackFromIntent();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPlayerView == null) return;

        PlayerManager pm = PlayerManager.getInstance();
        if (!pm.hasStream()) {
            // Le player a été libéré (app mise en arrière-plan puis restaurée).
            // On relance le stream depuis les extras de l'intent.
            startPlaybackFromIntent();
        } else {
            // Stream toujours actif : ré-attacher la vue seulement
            mPlayerView.setPlayer(pm.getPlayer(requireContext()));
        }
    }

    /**
     * Lance (ou relance) la lecture à partir des extras de l'intent de PlaybackActivity.
     * Appelé depuis onViewCreated ET depuis onResume si le stream a été interrompu.
     */
    private void startPlaybackFromIntent() {
        String url  = requireActivity().getIntent().getStringExtra(PlaybackActivity.CHANNEL_URL);
        String name = requireActivity().getIntent().getStringExtra(PlaybackActivity.CHANNEL_NAME);
        if (url != null && !url.isEmpty()) {
            PlayerManager pm = PlayerManager.getInstance();
            pm.play(requireContext(), url, name != null ? name : "");
            mPlayerView.setPlayer(pm.getPlayer(requireContext()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPlayerView != null) mPlayerView.setPlayer(null);
    }
}