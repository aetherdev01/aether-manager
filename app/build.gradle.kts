plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace   = "dev.aether.manager"
    compileSdk  = 36

    // ── Pin NDK version — wajib agar rv2ide pakai NDK yang sama ──────────
    ndkVersion  = "27.0.12077973"

    defaultConfig {
        applicationId = "dev.aether.manager"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 2
        versionName   = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── NDK — libaether-x.so ───────────────────────────────────────
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf(
                    "-DANDROID_STL=c++_static",
                    // ── Fix rv2ide: eksplisit set host tag agar path clang++ benar
                    "-DANDROID_HOST_TAG=linux-x86_64"
                )
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
            ndk { debugSymbolLevel = "NONE" }
        }
        debug {
            isMinifyEnabled = false
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation(libs.google.admob)
    debugImplementation(libs.androidx.ui.tooling)
}

// Safety net: block any transitive alpha/beta from pulling incompatible SDK versions
configurations.all {
    resolutionStrategy.eachDependency {
        val v = requested.version ?: return@eachDependency
        if (requested.group == "androidx.core" && (v.contains("alpha") || v.contains("beta"))) {
            useVersion("1.13.1")
            because("Block alpha/beta core")
        }
        if (requested.group == "androidx.activity" && (v.contains("alpha") || v.contains("beta"))) {
            useVersion("1.9.3")
            because("Block alpha/beta activity")
        }
    }
}
