import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ── Git short hash ─────────────────────────────────────────────────────────
val gitHash: String by lazy {
    try {
        providers.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        "unknown"
    }
}

android {
    namespace   = "dev.aether.manager"
    compileSdk  = 36

    defaultConfig {
        applicationId = "dev.aether.manager"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 130
        versionName   = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Signing ───────────────────────────────────────────────────────────
    signingConfigs {
        create("release") {
            val ks = rootProject.file("aether.jks")
            if (ks.exists()) {
                storeFile     = ks
                storePassword = System.getenv("STORE_PASSWORD") ?: ""
                keyAlias      = System.getenv("KEY_ALIAS")      ?: ""
                keyPassword   = System.getenv("KEY_PASSWORD")   ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            multiDexEnabled = false
        }
        debug {
            isMinifyEnabled = false
            multiDexEnabled = true
        }
    }

    // ── Rename APK → manager-{versionName}-{versionCode}-{gitHash}-{buildType}.apk
    applicationVariants.all {
        val variant = this
        outputs
            .map { it as BaseVariantOutputImpl }
            .forEach { output ->
                val vName     = variant.versionName
                val vCode     = variant.versionCode
                val buildType = variant.buildType.name
                output.outputFileName = "manager-$vName-$vCode-$gitHash-$buildType.apk"
            }
    }

    androidResources {
        generateLocaleConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.kotlin_module",
                "/META-INF/MANIFEST.MF",
                "**.proto",
                "kotlin/**",
                "META-INF/com/**"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
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
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.unity.ads)
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.io)
    implementation(libs.mmkv)
    implementation(libs.relinker)
    implementation(libs.zstd.jni)
    implementation(libs.sqlite.bundled)
    debugImplementation(libs.androidx.ui.tooling)
    
}