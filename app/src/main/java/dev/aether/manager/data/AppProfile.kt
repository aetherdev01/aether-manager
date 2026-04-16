package dev.aether.manager.data

data class AppInfo(
    val packageName: String,
    val label: String,
    val versionName: String,
    val targetSdk: Int,
    val isSystemApp: Boolean,
)

data class AppProfile(
    val packageName: String,
    val enabled: Boolean = false,
    val cpuGovernor: String = "default",
    val refreshRate: String = "default",
    val extraTweaks: AppExtraTweaks = AppExtraTweaks(),
)

data class AppExtraTweaks(
    val disableDoze: Boolean = false,
    val lockCpuMin: Boolean = false,
    val killBackground: Boolean = false,
    val gpuBoost: Boolean = false,
    val ioLatency: Boolean = false,
)

object CpuGovernors {
    val all = listOf("default", "performance", "powersave", "ondemand", "conservative", "schedutil", "interactive")
    val labels = mapOf(
        "default"       to "Default",
        "performance"   to "Performance",
        "powersave"     to "Powersave",
        "ondemand"      to "Ondemand",
        "conservative"  to "Conservative",
        "schedutil"     to "Schedutil",
        "interactive"   to "Interactive",
    )
    val primary = listOf("default", "performance", "powersave", "ondemand", "conservative")
}

object RefreshRates {
    val all = listOf("default", "60", "90", "120", "144", "165")
    val labels = mapOf(
        "default" to "Default",
        "60"  to "60 Hz",
        "90"  to "90 Hz",
        "120" to "120 Hz",
        "144" to "144 Hz",
        "165" to "165 Hz",
    )
}
