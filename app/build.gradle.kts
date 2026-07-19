plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.megaiptv.eltv"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.megaiptv.eltv"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // UI Android TV
    implementation(libs.leanback)

    // Images — OkHttp3 integration pour charger les logos via notre client SSL
    implementation(libs.glide.core)
    implementation(libs.glide.okhttp3)
    annotationProcessor(libs.glide.compiler)

    // Base de données Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Lecteur Media3/ExoPlayer — avec support OkHttp (SSL + timeout)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)

    // Réseau OkHttp (playlists M3U + SSL étendu)
    implementation(libs.okhttp.core)
}