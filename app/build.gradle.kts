plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.megaiptv.eltv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.megaiptv.eltv"
        minSdk = 31
        targetSdk = 34
        versionCode = 5
        versionName = "1.1.3"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
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

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.splashscreen)
    // UI Android TV
    implementation(libs.leanback)

    // Images — OkHttp3 integration pour charger les logos via notre client SSL
    implementation(libs.glide.core)
    implementation(libs.glide.okhttp3)
    annotationProcessor(libs.glide.compiler)

    // Base de données Room
    implementation(libs.roomRuntime)
    annotationProcessor(libs.roomCompiler)

    // Lecteur Media3/ExoPlayer — avec support OkHttp (SSL + timeout)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)

    // Réseau OkHttp (playlists M3U + SSL étendu)
    implementation(libs.okhttp.core)
}