package dev.aether.manager.i18n

val StringsId = AppStrings(
    // Splash
    splashSubtitle = "System Optimizer",
    splashStep0 = "Menginisialisasi sistem…",
    splashStep1 = "Memeriksa akses root…",
    splashStep2 = "Memuat konfigurasi…",
    splashStep3 = "Menyiapkan monitor…",
    splashStep4 = "Siap!",

    // Setup
    setupWelcomeTitle = "Selamat Datang!",
    setupWelcomeDesc = "Aether Manager adalah tool optimasi sistem Android yang dirancang untuk meningkatkan performa, mengelola penggunaan memori, dan menerapkan berbagai tweak sistem. Dengan kontrol yang mudah, kamu bisa membuat perangkat lebih cepat, stabil, dan responsif.",
    
    setupRootTitle = "Root Access",
    setupRootDesc = "Aplikasi ini membutuhkan akses root (Magisk / KernelSU / APatch). Pastikan izin superuser telah diberikan untuk Aether Manager.",
    setupRootCta = "Cek Root Sekarang",
    setupRootChecking = "Mengecek…",
    setupRootGranted = "Root berhasil diizinkan",
    setupRootDenied = "Root tidak ditemukan",
    setupRootDeniedSub = "Pastikan Magisk/KSU/APatch sudah terpasang",

    setupNotifTitle = "Izin Notifikasi",
    setupNotifDesc = "Izinkan notifikasi agar Aether Manager dapat memberi tahu saat tweak diterapkan atau ada pembaruan penting.",
    setupNotifCta = "Izinkan Notifikasi",
    setupNotifGranted = "Notifikasi diizinkan",
    setupNotifDenied = "Notifikasi ditolak",

    setupWriteTitle = "Pengaturan Sistem",
    setupWriteDesc = "Izin WRITE_SETTINGS diperlukan untuk menerapkan penyesuaian sistem dan mengubah pengaturan perangkat.",
    setupWriteCta = "Izinkan Pengaturan",
    setupWriteGranted = "Write Settings diizinkan",
    setupWriteDenied = "Write Settings ditolak",
    setupWriteDeniedSub = "Diperlukan untuk pengaturan sistem dan tweak performa",

    setupStorageTitle = "Izin Storage",
    setupStorageDesc = "Izin storage diperlukan untuk menyimpan log tweak dan backup konfigurasi ke penyimpanan perangkat.",
    setupStorageCta = "Izinkan Storage",
    setupStorageGranted = "Storage diizinkan",
    setupStorageDenied = "Storage ditolak",

    setupDoneTitle = "Siap Digunakan!",
    setupDoneDesc = "Setup selesai. Aether Manager siap mengoptimalkan performa, meningkatkan stabilitas, dan menerapkan berbagai tweak pada perangkat kamu.",

    setupBtnNext = "Lanjut",
    setupBtnStart = "Mulai",
    setupBtnBack = "Kembali",
    setupBtnSkip = "Lewati",
    setupBtnRetry = "Coba Lagi",
    setupRootRequired = "Izin root diperlukan untuk melanjutkan",

    // Nav
    navHome = "Beranda",
    navTweak = "Tweak",
    navLog = "Log",
    navAbout = "Tentang",

    // Home
    homeSystemStatus = "SYSTEM STATUS",
    homeMonitor = "Real-time Monitor",
    homeRefresh = "Refresh",
    homeRetry = "Coba Lagi",
    homeLabelCpu = "CPU",
    homeLabelGpu = "GPU",
    homeLabelRam = "RAM",
    homeLabelTemp = "SUHU",
    homeLabelStorage = "STORAGE",
    homeLabelUptime = "UPTIME",
    homeRamUsed = "%s digunakan",
    homeUptimeSince = "sejak boot terakhir",
    homeStorageOf = "dari %.1f GB",
    homeTempCpu = "CPU",
    homeTempBat = "Baterai",
    homeTempOverheat = "⚠ CPU overheat!",
    homeSelinux = "SELinux",
    homeProfile = "Profile",
    homeBootloopTitle = "Bootloop Guard Aktif",
    homeBootloopSub = "Boot count: %d",
    homeLabelOs = "OS",
    homeLabelKernel = "Kernel",
    homeLabelSoc = "SoC",
    homeLabelBattery = "Baterai",
    homeLabelSwap = "Swap",
    homeLabelGovernor = "Governor",
    homeLabelNetwork = "Jaringan",
    homeQuickInfo = "Info Perangkat",

    // Tweak
    tweakPerformanceProfile = "Performance Profile",
    tweakApplyAll = "Terapkan Semua Tweak",

    tweakSectionCpu = "CPU & Kernel",
    tweakSectionMemory = "Memori",
    tweakSectionIo = "I/O Scheduler",
    tweakSectionNetwork = "Jaringan",
    tweakSectionBattery = "Baterai & Harian",

    tweakSchedBoost = "Sched Boost",
    tweakSchedBoostDesc = "Meningkatkan prioritas scheduler CPU",

    tweakCpuBoost = "CPU Boost",
    tweakCpuBoostDesc = "Boost performa saat input (MTK/Snapdragon/Exynos)",

    tweakGpuThrottle = "GPU Throttle Off",
    tweakGpuThrottleDesc = "Menonaktifkan pembatasan performa GPU",

    tweakCpusetOpt = "CPUset Optimizer",
    tweakCpusetOptDesc = "Distribusi core untuk aplikasi aktif & background",

    tweakMtkBoost = "MTK EAS/HPS Boost",
    tweakMtkBoostDesc = "Optimasi hotplug & CCI (MediaTek)",

    tweakLmk = "LMK Agresif",
    tweakLmkDesc = "Menutup aplikasi background lebih cepat",

    tweakZram = "ZRAM",
    tweakZramDesc = "Swap terkompresi di RAM",

    tweakVmDirty = "VM Dirty Optimization",
    tweakVmDirtyDesc = "Optimasi parameter vm.dirty untuk I/O",

    tweakIoLatency = "I/O Latency Opt",
    tweakIoLatencyDesc = "Optimasi read_ahead dan latency disk",

    tweakTcpBbr = "TCP BBR",
    tweakTcpBbrDesc = "Algoritma jaringan lebih efisien",

    tweakNetBuffer = "Net Buffer Boost",
    tweakNetBufferDesc = "Meningkatkan buffer TCP",

    tweakDoze = "Doze Agresif",
    tweakDozeDesc = "Mempercepat masuk ke mode hemat daya",

    tweakClearCache = "Hapus Cache Boot",
    tweakClearCacheDesc = "Membersihkan cache saat boot",

    tweakFastAnim = "Animasi Cepat",
    tweakFastAnimDesc = "Skala animasi 0.5x",

    tweakEntropy = "Entropy Boost",
    tweakEntropyDesc = "Meningkatkan respons random generator",

    tweakZramSize = "Ukuran ZRAM",
    tweakZramAlgo = "Algoritma ZRAM",
    tweakIoScheduler = "I/O Scheduler",

    // About
    aboutApp = "Aplikasi",
    aboutVersion = "Versi",
    aboutMode = "Mode",
    aboutModeValue = "Standalone",
    aboutSupport = "Dukungan",
    aboutRoot = "Root",
    aboutSelinux = "SELinux",
    aboutSoc = "SoC",
    aboutGithub = "GitHub",
    aboutTelegram = "Channel",
    aboutSaweria = "saweria.co/AetherDev",
    aboutSaweriaLabel = "Saweria",
    aboutLicense = "Aether Manager adalah software gratis dan open source yang didistribusikan di bawah Lisensi MIT.",
    aboutTagModuleDev = "Module Dev",
    aboutTagOpenSource = "Open Source",
    aboutTagAndroid = "Android",

    // Log / Reboot
    logRebootOptions = "Opsi Reboot",
    logRebootSystem = "Reboot Sistem",
    logRebootSystemDesc = "Restart perangkat seperti biasa",
    logRebootRecovery = "Reboot Recovery",
    logRebootRecoveryDesc = "Masuk ke mode recovery",
    logReloadUi = "Reload UI",
    logReloadUiDesc = "Muat ulang tanpa reboot",
    logBtnCancel = "Batal",
)