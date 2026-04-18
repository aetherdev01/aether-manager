subprojects {
    configurations.all {
        resolutionStrategy {
            force("androidx.core:core:1.16.0")
            force("androidx.core:core-ktx:1.16.0")
            force("androidx.activity:activity:1.10.1")
            force("androidx.activity:activity-ktx:1.10.1")
            force("androidx.activity:activity-compose:1.10.1")
            force("androidx.navigation:navigation-compose:2.9.0")
            force("androidx.navigation:navigation-runtime-ktx:2.9.0")
            force("androidx.navigation:navigation-common-ktx:2.9.0")
        }
    }
}