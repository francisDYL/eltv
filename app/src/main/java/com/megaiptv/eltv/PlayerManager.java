package com.megaiptv.eltv;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public class PlayerManager {

    private static PlayerManager instance;
    private ExoPlayer player;

    public static PlayerManager getInstance() {
        if (instance == null) instance = new PlayerManager();
        return instance;
    }

    /**
     * Retourne (ou crée) le player singleton.
     * Le player utilise OkHttp comme couche réseau :
     *  - SSL étendu (trust-all) pour les sources IPTV non standard
     *  - Timeout de 30 s (connect / read / write)
     */
    public ExoPlayer getPlayer(Context context) {
        if (player == null) {
            // Factory OkHttp → confie tous les certificats + timeout 30 s
            OkHttpDataSource.Factory okHttpFactory =
                    new OkHttpDataSource.Factory(NetworkUtils.getClient());

            // Wrappée dans DefaultDataSource pour gérer les URI locales aussi
            DefaultDataSource.Factory dataSourceFactory =
                    new DefaultDataSource.Factory(context.getApplicationContext(), okHttpFactory);

            player = new ExoPlayer.Builder(context.getApplicationContext())
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                    .build();
        }
        return player;
    }

    /** Lance la lecture d'un flux. */
    public void play(Context context, String url) {
        ExoPlayer p = getPlayer(context);
        p.stop();
        p.clearMediaItems();
        p.setMediaItem(MediaItem.fromUri(url));
        p.prepare();
        p.play();
    }

    /** Libère le player (appelé quand PlaybackActivity se termine). */
    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
