plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// When the build provides a signing key, every build is signed with the same
// key, so a newer version installs over the older one.
val signingKeystore: String? = System.getenv("SIGNING_KEYSTORE")

android {
    namespace = "app.wakephrase"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.wakephrase"
        minSdk = 29
        targetSdk = 34
        versionCode = 4
        versionName = "3.1"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (signingKeystore != null) {
            create("fixed") {
                storeFile = file(signingKeystore!!)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (signingKeystore != null) {
                signingConfig = signingConfigs.getByName("fixed")
            }
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("com.alphacephei:vosk-android:0.3.47")
}
