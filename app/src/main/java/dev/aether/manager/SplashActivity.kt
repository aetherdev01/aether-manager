package dev.aether.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)
        val setupDone = prefs.getBoolean("setup_done", false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            AetherTheme {
                ProvideStrings {
                    SplashScreen {
                        if (setupDone) startActivity(Intent(this, MainActivity::class.java))
                        else startActivity(Intent(this, SetupActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface
    val secondary = MaterialTheme.colorScheme.secondary

    // Animatables — hanya yang essential
    val contentAlpha  = remember { Animatable(0f) }
    val progress      = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in semua konten sekaligus
        contentAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        // Progress bar sweep
        progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        delay(200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
            .alpha(contentAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_round),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(20.dp))

            // App name
            Text(
                "Aether Manager",
                color = onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(4.dp))

            // Subtitle
            Text(
                "Root Optimizer",
                color = primary.copy(alpha = 0.65f),
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(36.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(primary.copy(alpha = 0.5f), secondary)
                            )
                        )
                )
            }
        }

        // Version
        Text(
            "v1.4",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            color = onSurface.copy(alpha = 0.25f),
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        )
    }
}
