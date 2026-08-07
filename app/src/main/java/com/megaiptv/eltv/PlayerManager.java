package com.megaiptv.eltv;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

@UnstableApi
public class PlayerManager {
    
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    private static PlayerManager instance;
    private ExoPlayer player;

    // Métadonnées du flux en cours (utilisées par le mini-player)
    private String currentUrl;
    private String currentChannelName;

    public static PlayerManager getInstance() {
        if (instance == null) instance = new PlayerManager();
        return instance;
    }

    // ─── Création du player ───────────────────────────────────────────────────

    /**
     * Crée (ou retourne) le player ExoPlayer singleton.
     * Utilise OkHttp comme couche réseau : SSL étendu + timeout 30 s.
     */
    public ExoPlayer getPlayer(Context context) {
        if (player == null) {
            OkHttpDataSource.Factory okHttpFactory =
                    new OkHttpDataSource.Factory(NetworkUtils.getClient())
                            .setUserAgent(USER_AGENT);

            DefaultDataSource.Factory dataSourceFactory =
                    new DefaultDataSource.Factory(context.getApplicationContext(), okHttpFactory);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build();

            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context.getApplicationContext())
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                    .setEnableDecoderFallback(true); // Fallback to software if hardware fails

            DefaultTrackSelector trackSelector = new DefaultTrackSelector(context.getApplicationContext());
            trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                            .setTunnelingEnabled(false) // Tunneling can cause audio issues on some devices
                            .build()
            );

            player = new ExoPlayer.Builder(context.getApplicationContext())
                    .setRenderersFactory(renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                    .setAudioAttributes(audioAttributes, true)
                    .setHandleAudioBecomingNoisy(true)
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .build();

            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    android.util.Log.e("PlayerManager", "Playback Error: " + error.getErrorCodeName() + " - " + error.getMessage());
                }

                @Override
                public void onTracksChanged(@NonNull Tracks tracks) {
                    android.util.Log.d("PlayerManager", "Tracks changed. Has audio: " + tracks.isTypeSelected(C.TRACK_TYPE_AUDIO));
                }
            });
        }
        return player;
    }

    /**
     * Retourne le player S'IL EXISTE déjà, sans en créer un nouveau.
     * Utilisé par le mini-player pour savoir si un flux est actif.
     */
    public ExoPlayer getPlayerIfExists() {
        return player;
    }

    // ─── Lecture ──────────────────────────────────────────────────────────────

    /** Lance la lecture et mémorise l'URL + le nom de la chaîne. */
    public void play(Context context, String url, String channelName) {
        this.currentUrl         = url;
        this.currentChannelName = channelName;

        ExoPlayer p = getPlayer(context);
        p.stop();
        p.clearMediaItems();
        p.setMediaItem(MediaItem.fromUri(url));
        p.setVolume(1.0f); // Ensure volume is explicitly enabled
        p.prepare();
        p.play();
    }

    // ─── Contrôle de la lecture ───────────────────────────────────────────────

    /** Met en pause la lecture sans libérer le player. */
    public void pause() {
        if (player != null) player.pause();
    }

    /** Reprend la lecture si elle était en pause. */
    public void resume() {
        if (player != null) player.play();
    }

    // ─── État ─────────────────────────────────────────────────────────────────

    /** Vrai si un flux est chargé et actif (lecture ou chargement). */
    public boolean isStreamActive() {
        if (player == null) return false;
        int state = player.getPlaybackState();
        return state == Player.STATE_READY || state == Player.STATE_BUFFERING;
    }

    /** Vrai si le player est en cours de lecture (pas en pause). */
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    /** Vrai si un flux a été chargé (même en pause). */
    public boolean hasStream() {
        return currentUrl != null && !currentUrl.isEmpty();
    }

    public String getCurrentUrl()         { return currentUrl; }
    public String getCurrentChannelName() { return currentChannelName; }

    // ─── Libération ───────────────────────────────────────────────────────────

    /** Libère le player (appelé à la fermeture de PlaybackActivity). */
    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        currentUrl         = null;
        currentChannelName = null;
    }
}
