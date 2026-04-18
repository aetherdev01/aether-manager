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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.PI

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
    val primary    = MaterialTheme.colorScheme.primary
    val onSurface  = MaterialTheme.colorScheme.onSurface
    val surface    = MaterialTheme.colorScheme.surface
    val secondary  = MaterialTheme.colorScheme.secondary
    val tertiary   = MaterialTheme.colorScheme.tertiary

    // ── Animatables ───────────────────────────────────────────
    val bgAlpha       = remember { Animatable(0f) }
    val orbScale      = remember { Animatable(0.6f) }
    val orbAlpha      = remember { Animatable(0f) }
    val iconScale     = remember { Animatable(0.4f) }
    val iconAlpha     = remember { Animatable(0f) }
    val titleAlpha    = remember { Animatable(0f) }
    val titleOffsetY  = remember { Animatable(24f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(16f) }
    val barProgress   = remember { Animatable(0f) }
    val barAlpha      = remember { Animatable(0f) }
    val versionAlpha  = remember { Animatable(0f) }

    // Orb breathing animation (infinite)
    val infiniteTransition = rememberInfiniteTransition(label = "orb_breathe")
    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )
    val orbRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "orb_rotate"
    )

    // Shimmer sweep on title
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing, delayMillis = 800),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    LaunchedEffect(Unit) {
        // 1. Background fade
        launch { bgAlpha.animateTo(1f, tween(300)) }

        // 2. Orb bloom
        delay(100)
        launch {
            orbAlpha.animateTo(1f, tween(500))
            orbScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }

        // 3. Icon pop-in dengan spring bounce
        delay(300)
        launch {
            iconAlpha.animateTo(1f, tween(300))
            iconScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            )
        }

        // 4. Title slide-up + fade
        delay(550)
        launch {
            titleAlpha.animateTo(1f, tween(400))
            titleOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }

        // 5. Subtitle stagger
        delay(700)
        launch {
            subtitleAlpha.animateTo(1f, tween(350))
            subtitleOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }

        // 6. Progress bar
        delay(850)
        launch { barAlpha.animateTo(1f, tween(250)) }
        launch { barProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }

        // 7. Version
        delay(950)
        launch { versionAlpha.animateTo(1f, tween(300)) }

        // 8. Selesai
        delay(1400)
        onFinished()
    }

    val context = LocalContext.current
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        catch (_: Exception) { "?" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface.copy(alpha = bgAlpha.value)),
        contentAlignment = Alignment.Center
    ) {

        // ── Background orb glow ───────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    alpha = orbAlpha.value,
                    scaleX = orbScale.value * orbPulse,
                    scaleY = orbScale.value * orbPulse,
                )
                .blur(48.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rad = size.minDimension * 0.45f

            // Rotating duo orbs
            val angle1 = orbRotate * (PI / 180f).toFloat()
            val angle2 = angle1 + PI.toFloat()
            val dist   = rad * 0.28f

            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx + dist * sin(angle1), cy + dist * sin(angle1 * 0.7f)),
                    radius = rad * 0.7f
                ),
                radius = rad * 0.7f,
                center = Offset(cx + dist * sin(angle1), cy + dist * sin(angle1 * 0.7f))
            )
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx + dist * sin(angle2), cy + dist * sin(angle2 * 0.7f)),
                    radius = rad * 0.55f
                ),
                radius = rad * 0.55f,
                center = Offset(cx + dist * sin(angle2), cy + dist * sin(angle2 * 0.7f))
            )
            // Soft center bloom
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(tertiary.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = rad * 0.5f
                ),
                radius = rad * 0.5f,
                center = Offset(cx, cy)
            )
        }

        // ── Main content ──────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Icon dengan ring glow
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .graphicsLayer(
                            alpha  = iconAlpha.value * 0.5f,
                            scaleX = iconScale.value,
                            scaleY = iconScale.value,
                        )
                        .blur(16.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(primary.copy(alpha = 0.6f), Color.Transparent))
                        )
                )
                // Icon
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer(
                            scaleX = iconScale.value,
                            scaleY = iconScale.value,
                            alpha  = iconAlpha.value
                        )
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_round),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── App name dengan shimmer + warna per kata ──────
            Box(
                modifier = Modifier.graphicsLayer(
                    alpha         = titleAlpha.value,
                    translationY  = titleOffsetY.value.dp.value
                )
            ) {
                // Shimmer sweep brush
                val shimmerBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f       to Color.Transparent,
                        (shimmer - 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                        shimmer.coerceIn(0f, 1f)           to Color.White.copy(alpha = 0.55f),
                        (shimmer + 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                        1f       to Color.Transparent,
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(1000f, 0f)
                )

                // Base text — "Aether" hitam/onSurface kuat, "Manager" ikut M3 primary
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color      = onSurface,
                                fontWeight = FontWeight.Black,
                            )
                        ) { append("Aether") }
                        append(" ")
                        withStyle(
                            SpanStyle(
                                color      = primary,
                                fontWeight = FontWeight.Light,
                            )
                        ) { append("Manager") }
                    },
                    fontSize      = 32.sp,
                    letterSpacing = (-0.8).sp
                )

                // Shimmer overlay — sama persis tapi pakai brush transparan
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(brush = shimmerBrush, fontWeight = FontWeight.Black)) {
                            append("Aether")
                        }
                        append(" ")
                        withStyle(SpanStyle(brush = shimmerBrush, fontWeight = FontWeight.Light)) {
                            append("Manager")
                        }
                    },
                    fontSize      = 32.sp,
                    letterSpacing = (-0.8).sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                "Optimizing Your Android Experience",
                modifier = Modifier.graphicsLayer(
                    alpha        = subtitleAlpha.value,
                    translationY = subtitleOffsetY.value.dp.value
                ),
                color         = primary.copy(alpha = 0.6f),
                fontSize      = 11.5.sp,
                letterSpacing = 0.6.sp,
                fontWeight    = FontWeight.Medium
            )

            Spacer(Modifier.height(44.dp))

            // ── Progress bar dengan glow ──────────────────────
            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = barAlpha.value)
                    .width(160.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.10f))
            ) {
                // Glow layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barProgress.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .blur(4.dp)
                        .background(
                            Brush.horizontalGradient(listOf(primary.copy(alpha = 0.4f), secondary.copy(alpha = 0.6f)))
                        )
                )
                // Solid bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barProgress.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(primary, secondary))
                        )
                )
            }
        }

        // ── Version ───────────────────────────────────────────
        Text(
            "v$versionName",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .graphicsLayer(alpha = versionAlpha.value),
            color         = onSurface.copy(alpha = 0.22f),
            fontSize      = 10.sp,
            letterSpacing = 1.5.sp,
            fontWeight    = FontWeight.Medium
        )
    }
}