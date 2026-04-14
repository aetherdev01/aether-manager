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
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.ui.about.AboutScreen
import dev.aether.manager.ui.components.RebootBottomSheet
import dev.aether.manager.ui.home.HomeScreen
import dev.aether.manager.ui.tweak.TweakScreen
import dev.aether.manager.util.RootUtils

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AetherTheme {
                ProvideStrings {
                    AetherApp(vm)
                }
            }
        }
    }
}

private enum class Screen { HOME, TWEAK, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherApp(vm: MainViewModel) {
    val s = LocalStrings.current
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showReboot by remember { mutableStateOf(false) }
    val snack by vm.snackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Nav items rebuild when language changes (derived from strings)
    val navLabels = listOf(s.navHome, s.navTweak, s.navAbout)
    val navScreens = listOf(Screen.HOME, Screen.TWEAK, Screen.ABOUT)
    val navSelectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Tune, Icons.Filled.Info)
    val navUnselectedIcons = listOf(Icons.Outlined.Home, Icons.Outlined.Tune, Icons.Outlined.Info)

    LaunchedEffect(snack) {
        if (snack != null) {
            snackbarHostState.showSnackbar(snack!!, duration = SnackbarDuration.Short)
            vm.clearSnack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Aether Manager", fontWeight = FontWeight.Medium, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = { showReboot = true }) {
                        Icon(Icons.Outlined.RestartAlt, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
                navScreens.forEachIndexed { idx, screen ->
                    val selected = currentScreen == screen
                    val scale by animateFloatAsState(
                        if (selected) 1.1f else 1f,
                        spring(Spring.DampingRatioMediumBouncy), label = "tab_scale_$idx"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Box(Modifier.scale(scale)) {
                                Icon(if (selected) navSelectedIcons[idx] else navUnselectedIcons[idx], null)
                            }
                        },
                        label = { Text(navLabels[idx], fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
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
                    Screen.ABOUT -> AboutScreen(vm)
                }
            }
        }
    }

    if (showReboot) {
        RebootBottomSheet(
            onDismiss = { showReboot = false },
            onReboot = { vm.reboot(RootUtils.RebootMode.NORMAL) },
            onRebootRecovery = { vm.reboot(RootUtils.RebootMode.RECOVERY) },
            onReloadUI = { vm.refresh() }
        )
    }
}
