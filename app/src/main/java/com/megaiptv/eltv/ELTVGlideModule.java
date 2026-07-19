package com.megaiptv.eltv;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

/**
 * Branche Glide sur notre OkHttpClient personnalisé.
 *
 * Sans ce module, Glide utilise HttpUrlConnection qui :
 *  - Rejette les certificats SSL auto-signés (logos IPTV très souvent concernés)
 *  - N'a pas de timeout configurable
 *
 * Avec ce module, Glide hérite de :
 *  - SSL trust-all  (tous les certificats acceptés)
 *  - Timeout 30 s   (connect / read / write)
 */
@GlideModule
public final class ELTVGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(@NonNull Context context,
                                   @NonNull Glide glide,
                                   @NonNull Registry registry) {
        // Remplace le loader HTTP par défaut par notre client OkHttp
        OkHttpUrlLoader.Factory factory =
                new OkHttpUrlLoader.Factory(NetworkUtils.getClient());

        registry.replace(GlideUrl.class, InputStream.class, factory);
    }

    // Désactive la découverte automatique (pas d'autre GlideModule dans le projet)
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}

