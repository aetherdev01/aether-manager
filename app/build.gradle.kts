import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ── Git short hash ─────────────────────────────────────────────────────────
val gitHash: String by lazy {
    try {
        val out = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
            standardOutput = out
        }
        out.toString().trim()
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
    // ── Core ──────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // ── Lifecycle & ViewModel ─────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)   // collectAsStateWithLifecycle

    // ── Compose ───────────────────────────────────────────────────────────
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ── Coroutines ────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── DataStore ─────────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── Accompanist ───────────────────────────────────────────────────────
    implementation(libs.accompanist.systemuicontroller)

    // ── Splash Screen ─────────────────────────────────────────────────────
    implementation(libs.androidx.core.splashscreen)

    // ── Image Loading (Coil) ──────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── Networking (OkHttp) ───────────────────────────────────────────────
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)                        // log request/response di debug

    // ── JSON (Gson) ───────────────────────────────────────────────────────
    implementation(libs.gson)

    // ── Background Work (WorkManager) ─────────────────────────────────────
    implementation(libs.androidx.work.runtime.ktx)

    // ── Ads ───────────────────────────────────────────────────────────────
    implementation(libs.unity.ads)

    // ── Native Libraries (.so) ────────────────────────────────────────────
    // libsu — root shell execution (libsu-core.so), ganti NativeExec manual
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)                         // optional: root service binding
    implementation(libs.libsu.io)                              // optional: root file I/O

    // MMKV — key-value storage native (libmmkv.so), lebih cepat dari DataStore
    implementation(libs.mmkv)

    // ReLinker — safe .so loader, cocok untuk libaether-x.so
    implementation(libs.relinker)

    // Zstd-jni — kompresi native (libzstd-jni.so), untuk backup file
    implementation(libs.zstd.jni)

    // sqlite-android — SQLite native terbaru (libsqliteX.so)
    implementation(libs.sqlite.android)

    // ── Debug ─────────────────────────────────────────────────────────────
    debugImplementation(libs.androidx.ui.tooling)
}
