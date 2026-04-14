// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
}

// Force semua subproject pakai versi stable — cegah transitive alpha/beta masuk
subprojects {
    configurations.all {
        resolutionStrategy {
            // Force core ke stable, blokir alpha
            force("androidx.core:core:1.13.1")
            force("androidx.core:core-ktx:1.13.1")
            // Force activity ke versi yang butuh compileSdk 35 max
            force("androidx.activity:activity:1.9.3")
            force("androidx.activity:activity-ktx:1.9.3")
            force("androidx.activity:activity-compose:1.9.3")
            // Force navigation ke stable non-alpha
            force("androidx.navigation:navigation-compose:2.7.7")
            force("androidx.navigation:navigation-runtime-ktx:2.7.7")
            force("androidx.navigation:navigation-common-ktx:2.7.7")
        }
    }
}
