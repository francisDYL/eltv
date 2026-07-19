package com.megaiptv.eltv;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

/**
 * Application globale : surveille le cycle de vie de toutes les activités.
 *
 * Logique de comptage (onStart / onStop) :
 *   - Navigation interne (A → B) : count passe 1 → 2 → 1  (jamais 0)
 *   - App mise en arrière-plan    : count passe 1 → 0 → STOP
 *   - Retour au premier plan      : count passe 0 → 1 → mini-player se met à jour
 *
 * Ainsi le stream s'arrête automatiquement quand l'utilisateur appuie sur
 * Home, bascule vers une autre application (Netflix, etc.) ou éteint l'écran.
 */
public class ELTVApplication extends Application {

    /** Nombre d'activités actuellement en état "démarré" (entre onStart et onStop). */
    private int startedCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                startedCount++;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                // Ignorer les changements de configuration (rotation, etc.)
                if (activity.isChangingConfigurations()) return;

                startedCount = Math.max(0, startedCount - 1);

                if (startedCount == 0) {
                    // Aucune activité en premier plan → app en arrière-plan
                    // Arrêt immédiat du stream
                    PlayerManager.getInstance().release();
                }
            }

            // ── Callbacks non utilisés ─────────────────────────────────────
            @Override public void onActivityCreated(@NonNull Activity a, Bundle b) {}
            @Override public void onActivityResumed(@NonNull Activity a) {}
            @Override public void onActivityPaused(@NonNull Activity a) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
            @Override public void onActivityDestroyed(@NonNull Activity a) {}
        });
    }
}

