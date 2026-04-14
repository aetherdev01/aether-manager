plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace   = "dev.aether.manager"
    compileSdk  = 36
    // Gunakan NDK yang ter-install — cek via: ls $ANDROID_HOME/ndk/
    // Uncomment dan sesuaikan jika perlu:
    // ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "dev.aether.manager"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 2
        versionName   = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── NDK — libaether-x.so ───────────────────────────────────────
        ndk {
            // arm64-v8a : utama (Snapdragon modern, perangkat utama)
            // armeabi-v7a : kompatibilitas perangkat lama (opsional)
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_static"
            }
        }
    }

    // ── CMake build untuk libaether-x.so ──────────────────────────────────
    externalNativeBuild {
        cmake {
            path    = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Strip debug symbols dari .so di release
            ndk { debugSymbolLevel = "NONE" }
        }
        debug {
            isMinifyEnabled = false
            // Simpan debug symbols untuk ADB attach
            ndk { debugSymbolLevel = "FULL" }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // useLegacyPackaging false = .so di-compress dalam APK (lebih kecil)
        // strip otomatis di-handle oleh AGP + CMake saat release build
        jniLibs {
            useLegacyPackaging = true 
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.appcompat)
    debugImplementation(libs.androidx.ui.tooling)
}

// Force versi stable — blokir alpha/beta yang masuk via BOM transitif
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")
        force("androidx.activity:activity:1.9.3")
        force("androidx.activity:activity-ktx:1.9.3")
        force("androidx.activity:activity-compose:1.9.3")
        force("androidx.navigation:navigation-compose:2.7.7")
        force("androidx.navigation:navigation-runtime-ktx:2.7.7")
        force("androidx.navigation:navigation-common-ktx:2.7.7")
        force("androidx.navigation:navigation-common:2.7.7")
        force("androidx.navigationevent:navigationevent-android:1.0.0-beta01")
        eachDependency {
            val v = requested.version ?: return@eachDependency
            // Paksa core ke stable jika ada yang meminta alpha
            if (requested.group == "androidx.core") {
                if (v.contains("alpha") || v.contains("beta")) {
                    useVersion("1.13.1")
                    because("Block alpha/beta core — compileSdk 35 max")
                }
            }
            // Paksa activity ke stable
            if (requested.group == "androidx.activity") {
                if (v.contains("alpha") || v.contains("beta")) {
                    useVersion("1.9.3")
                    because("Block alpha/beta activity — compileSdk 35 max")
                }
            }
            // Block navigationevent yang butuh SDK 36
            if (requested.group == "androidx.navigationevent") {
                useVersion("1.0.0-beta01")
                because("navigationevent 1.0.0 stable butuh SDK 36, paksa beta yang compat")
            }
        }
    }
}
