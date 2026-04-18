package dev.aether.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.aether.manager.ads.AdScheduler
import dev.aether.manager.ads.InterstitialAdManager
import dev.aether.manager.data.AppProfileViewModel
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.gamebooster.GameBoosterManager
import dev.aether.manager.i18n.LanguageDropdownCompact
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.ui.about.AboutScreen
import dev.aether.manager.ui.appprofile.AppProfileScreen
import dev.aether.manager.ui.components.IosToastHost
import dev.aether.manager.ui.components.IosToastType
import dev.aether.manager.ui.components.RebootBottomSheet
import dev.aether.manager.ui.components.rememberIosToastState
import dev.aether.manager.ui.home.HomeScreen
import dev.aether.manager.ui.settings.SettingsScreen
import dev.aether.manager.ui.tweak.TweakScreen
import dev.aether.manager.update.UpdateDialogHost
import dev.aether.manager.update.UpdateViewModel
import dev.aether.manager.util.RootUtils

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val apVm: AppProfileViewModel by viewModels()
    private val updateVm: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AetherTheme {
                ProvideStrings {
                    AetherApp(vm, apVm, updateVm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Sync toggle state setelah kembali dari Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    }
}

private enum class Screen { HOME, TWEAK, APPS, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherApp(vm: MainViewModel, apVm: AppProfileViewModel, updateVm: UpdateViewModel) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showReboot    by remember { mutableStateOf(false) }
    var showSettings  by remember { mutableStateOf(false) }

    // ── Game Booster state ────────────────────────────────────────────────
    var boosterActive         by remember { mutableStateOf(GameBoosterManager.isRunning()) }
    var showBoosterPermDialog by remember { mutableStateOf(false) }
    // Sync saat Activity resume (dari overlay permission screen)
    val lifecycleOwner2 = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner2) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                boosterActive = GameBoosterManager.isRunning()
            }
        }
        lifecycleOwner2.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner2.lifecycle.removeObserver(obs) }
    }

    fun toggleBooster() {
        if (boosterActive) {
            GameBoosterManager.stopBooster(context)
            boosterActive = false
        } else {
            if (!GameBoosterManager.isOverlayPermissionGranted(context)) {
                showBoosterPermDialog = true
            } else {
                GameBoosterManager.startBooster(context)
                boosterActive = true
            }
        }
    }

    // ── Interstitial Ads ──────────────────────────────────────────────────
    val activity = context as android.app.Activity
    LaunchedEffect(currentScreen) { InterstitialAdManager.showIfReady(activity) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> AdScheduler.start { activity.takeUnless { it.isFinishing || it.isDestroyed } }
                Lifecycle.Event.ON_PAUSE  -> AdScheduler.stop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Toast ─────────────────────────────────────────────────────────────
    val snack    by vm.snackMessage.collectAsState()
    val applying by vm.applyingTweak.collectAsState()
    val iosToast = rememberIosToastState()

    LaunchedEffect(applying) {
        if (applying) iosToast.showLoading("Menerapkan tweaks…")
    }
    LaunchedEffect(snack) {
        if (snack != null) {
            val isError = snack!!.startsWith("Gagal") || snack!!.startsWith("Error")
            iosToast.resolve(
                message = snack!!,
                type    = if (isError) IosToastType.ERROR else IosToastType.SUCCESS
            )
            vm.clearSnack()
        }
    }

    data class NavItem(
        val screen: Screen,
        val label: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector,
    )
    val navItems = listOf(
        NavItem(Screen.HOME,  s.navHome,  Icons.Filled.Home,  Icons.Outlined.Home),
        NavItem(Screen.TWEAK, s.navTweak, Icons.Filled.Tune,  Icons.Outlined.Tune),
        NavItem(Screen.APPS,  s.navApps,  Icons.Filled.Apps,  Icons.Outlined.Apps),
        NavItem(Screen.ABOUT, s.navAbout, Icons.Filled.Info,  Icons.Outlined.Info),
    )

    // ── Game Booster Permission Dialog ────────────────────────────────────
    if (showBoosterPermDialog) {
        AlertDialog(
            onDismissRequest = { showBoosterPermDialog = false },
            icon = {
                Icon(Icons.Filled.Gamepad, null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Izin Diperlukan") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Game Booster membutuhkan izin untuk tampil di atas aplikasi lain " +
                                "(SYSTEM_ALERT_WINDOW) agar dapat menampilkan sidebar overlay saat game berjalan.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PermRow(icon = Icons.Outlined.Layers,           label = "Tampil di atas layar (Overlay)")
                            PermRow(icon = Icons.Outlined.QueryStats,       label = "Akses statistik penggunaan")
                            PermRow(icon = Icons.Outlined.NotificationsOff, label = "Kelola DND / notifikasi")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showBoosterPermDialog = false
                    GameBoosterManager.requestOverlayPermission(activity)
                }) {
                    Text("Buka Pengaturan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBoosterPermDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // ── SettingsScreen overlay ────────────────────────────────────────────
    if (showSettings) {
        SettingsScreen(
            vm              = vm,
            onBack          = { showSettings = false },
            onResetProfiles = { apVm.resetAllProfiles() },
            onResetMonitor  = { apVm.resetMonitor() }
        )
        return
    }

    // ── Main scaffold ─────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aether Manager", fontWeight = FontWeight.Medium, fontSize = 20.sp) },
                actions = {
                    LanguageDropdownCompact()

                    // ── Game Booster toggle ──────────────────────────────
                    val boosterTint = if (boosterActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant

                    IconButton(onClick = { toggleBooster() }) {
                        Icon(
                            imageVector = if (boosterActive) Icons.Filled.Gamepad else Icons.Outlined.Gamepad,
                            contentDescription = if (boosterActive) "Matikan Game Booster" else "Aktifkan Game Booster",
                            tint = boosterTint
                        )
                    }

                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, null)
                    }
                    IconButton(onClick = { showReboot = true }) {
                        Icon(Icons.Outlined.RestartAlt, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
                navItems.forEachIndexed { idx, item ->
                    val selected = currentScreen == item.screen
                    val scale by animateFloatAsState(
                        if (selected) 1.1f else 1f,
                        spring(Spring.DampingRatioMediumBouncy), label = "tab_scale_$idx"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick  = { currentScreen = item.screen },
                        icon = {
                            Box(Modifier.scale(scale)) {
                                Icon(if (selected) item.selectedIcon else item.unselectedIcon, null)
                            }
                        },
                        label  = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            indicatorColor      = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues).fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally { it * dir / 4 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { -it * dir / 4 } + fadeOut(tween(150)))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.HOME  -> HomeScreen(vm)
                    Screen.TWEAK -> TweakScreen(vm)
                    Screen.APPS  -> AppProfileScreen(apVm)
                    Screen.ABOUT -> AboutScreen(vm = vm)
                }
            }
            IosToastHost(iosToast)
        }
    }

    if (showReboot) {
        RebootBottomSheet(
            onDismiss        = { showReboot = false },
            onReboot         = { vm.reboot(RootUtils.RebootMode.NORMAL) },
            onRebootRecovery = { vm.reboot(RootUtils.RebootMode.RECOVERY) },
            onReloadUI       = { vm.refresh() }
        )
    }

    UpdateDialogHost(viewModel = updateVm)
}

@Composable
private fun PermRow(icon: ImageVector, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
