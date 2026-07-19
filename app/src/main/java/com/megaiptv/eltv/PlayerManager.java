package com.megaiptv.eltv;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public class PlayerManager {

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
                    new OkHttpDataSource.Factory(NetworkUtils.getClient());
            DefaultDataSource.Factory dataSourceFactory =
                    new DefaultDataSource.Factory(context.getApplicationContext(), okHttpFactory);

            player = new ExoPlayer.Builder(context.getApplicationContext())
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                    .build();
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
        p.prepare();
        p.play();
    }

    // ─── État ─────────────────────────────────────────────────────────────────

    /** Vrai si un flux est chargé et actif (lecture ou chargement). */
    public boolean isStreamActive() {
        if (player == null) return false;
        int state = player.getPlaybackState();
        return state == Player.STATE_READY || state == Player.STATE_BUFFERING;
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
