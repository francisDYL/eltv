package com.megaiptv.eltv;

import android.util.Log;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkUtils {

    private static final String TAG     = "NetworkUtils";
    private static final int    TIMEOUT = 30; // secondes

    private static OkHttpClient client;
    private static X509TrustManager trustManager;

    /** Trust-manager qui accepte tous les certificats (sources IPTV non standard). */
    public static X509TrustManager getTrustManager() {
        if (trustManager == null) {
            trustManager = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };
        }
        return trustManager;
    }

    /** Client OkHttp partagé : SSL étendu + timeout 30 s + User-Agent standard. */
    public static OkHttpClient getClient() {
        if (client == null) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{getTrustManager()}, new SecureRandom());

                client = new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), getTrustManager())
                        .hostnameVerifier((hostname, session) -> true)
                        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                .build()))
                        .build();
            } catch (Exception e) {
                Log.e(TAG, "SSL setup failed, falling back to default client", e);
                client = new OkHttpClient.Builder()
                        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .build();
            }
        }
        return client;
    }

    /** Télécharge le contenu d'une URL (utilisé pour les playlists M3U). */
    public static String fetchUrl(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            if (response.body() == null) throw new IOException("Empty response body");
            return response.body().string();
        }
    }
}
