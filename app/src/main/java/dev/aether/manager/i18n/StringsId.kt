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
    setupLangTitle = "Pilih Bahasa",
    setupLangDesc = "Pilih bahasa yang ingin kamu gunakan. Bisa diganti kapan saja melalui Pengaturan.",
    
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
    setupNotifDenied = "Notifikasi ditolak — wajib diizinkan",

    setupWriteTitle = "Pengaturan Sistem",
    setupWriteDesc = "Izin WRITE_SETTINGS diperlukan untuk menerapkan penyesuaian sistem dan mengubah pengaturan perangkat.",
    setupWriteCta = "Izinkan Pengaturan",
    setupWriteGranted = "Write Settings diizinkan",
    setupWriteDenied = "Write Settings ditolak",
    setupWriteDeniedSub = "Izin ini wajib diberikan untuk melanjutkan.",

    setupStorageTitle = "Izin Storage",
    setupStorageDesc = "Izin storage diperlukan untuk menyimpan log tweak dan backup konfigurasi ke penyimpanan perangkat.",
    setupStorageCta = "Izinkan Storage",
    setupStorageGranted = "Storage diizinkan",
    setupStorageDenied = "Storage ditolak — wajib diizinkan",

    setupDoneTitle = "Siap Digunakan!",
    setupDoneDesc = "Setup selesai. Aether Manager siap mengoptimalkan performa, meningkatkan stabilitas, dan menerapkan berbagai tweak pada perangkat kamu.",
    setupIncompleteTitle = "Lengkapi Setup Dulu!",
    setupIncompleteDesc = "Beberapa izin yang diperlukan belum diberikan. Kembali dan selesaikan semua langkah sebelum melanjutkan.",

    setupBtnNext = "Lanjut",
    setupBtnStart = "Mulai",
    setupBtnBack = "Kembali",
    setupBtnSkip = "Lewati",
    setupBtnRetry = "Coba Lagi",
    setupRootRequired = "Izin ini wajib diberikan untuk melanjutkan",

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

    tweakDoh = "DNS over HTTPS",
    tweakDohDesc = "Enkripsi DNS untuk privasi dan kecepatan lebih baik",

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
    aboutSectionDev = "Developer",
    aboutSectionAppInfo = "Informasi Aplikasi",
    aboutSectionLinks = "Komunitas & Tautan",
    aboutDevDesc = "Android & Root Module Developer",
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

    // Settings
    settingsTitle = "Pengaturan",
    settingsSectionBackup = "Backup & Reset",
    settingsBtnBackup = "Backup",
    settingsBtnResetDefault = "Reset Default",
    settingsNoBackup = "Belum ada backup tersimpan",
    settingsResetTitle = "Reset ke Default?",
    settingsResetDesc = "Semua tweak dinonaktifkan dan nilai sistem dikembalikan ke default Android. File backup yang ada tidak terhapus.",
    settingsResetConfirm = "Reset",
    settingsBtnCancel = "Batal",
    settingsRestoreTitle = "Restore Backup?",
    settingsRestoreDesc = "Setting aktif diganti dengan backup ini dan langsung diterapkan ke sistem.",
    settingsRestoreConfirm = "Restore",
    settingsBackupProfile = "Profile: %s",
    settingsBtnDelete = "Hapus",

    // Nav (extra)
    navApps = "Aplikasi",

    // Update dialog
    updateAvailable = "Update Tersedia",
    updateTabDesc = "Deskripsi",
    updateTabChangelog = "Changelog",
    updateBtnDownload = "Download & Install",
    updateBtnLater = "Nanti Saja",
    updateBtnRetry = "Coba Lagi",
    updateBtnBrowser = "Buka di Browser",
    updateInstalling = "Menginstall Pembaruan…",
    updateFailed = "Gagal: %s",
    updateDownloadDone = "Selesai diunduh",
    updateDownloading = "Mengunduh APK…",
    updateAboutTitle = "Tentang Update Ini",
    updateAboutDesc = "Versi baru Aether Manager telah tersedia. Update direkomendasikan untuk mendapatkan fitur terbaru, perbaikan bug, dan peningkatan performa.",
    updateChangelogLoading = "Memuat changelog…",
    updateChangelogEmpty = "Tidak ada changelog tersedia.",

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