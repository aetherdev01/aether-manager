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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.data.AppProfileViewModel
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.LanguageDropdownCompact
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.ui.about.AboutScreen
import dev.aether.manager.ui.appprofile.AppProfileScreen
import dev.aether.manager.ui.components.RebootBottomSheet
import dev.aether.manager.ui.home.HomeScreen
import dev.aether.manager.ui.settings.SettingsScreen
import dev.aether.manager.ui.tweak.TweakScreen
import androidx.compose.ui.platform.LocalContext
import dev.aether.manager.ads.RewardedAdManager
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
}

private enum class Screen { HOME, TWEAK, APPS, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherApp(vm: MainViewModel, apVm: AppProfileViewModel, updateVm: UpdateViewModel) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var currentScreen  by remember { mutableStateOf(Screen.HOME) }
    var showReboot     by remember { mutableStateOf(false) }
    var showSettings   by remember { mutableStateOf(false) }

    // Rewarded ad: tampil saat user buka tab APPS (tidak mengganggu, hanya sekali per kunjungan)
    val activity = context as android.app.Activity
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.APPS) {
            RewardedAdManager.showIfReady(activity)
        }
    }

    val snack by vm.snackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(snack) {
        if (snack != null) {
            snackbarHostState.showSnackbar(snack!!, duration = SnackbarDuration.Short)
            vm.clearSnack()
        }
    }

    // ── SettingsScreen overlay (full-screen, di atas Scaffold) ────────────
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Aether Manager", fontWeight = FontWeight.Medium, fontSize = 20.sp) },
                actions = {
                    LanguageDropdownCompact()
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, null)
                    }
                    IconButton(onClick = {
                        showReboot = true
                    }) {
                        Icon(Icons.Outlined.RestartAlt, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = MaterialTheme.colorScheme.surface,
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

    // Update dialog — selalu di atas semua konten
    UpdateDialogHost(viewModel = updateVm)
}
