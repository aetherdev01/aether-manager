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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val s = LocalStrings.current
    val steps = listOf(s.splashStep0, s.splashStep1, s.splashStep2, s.splashStep3, s.splashStep4)

    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val secondary = MaterialTheme.colorScheme.secondary

    val scope = rememberCoroutineScope()
    val glowAlpha     = remember { Animatable(0f) }
    val ringScale     = remember { Animatable(0.6f) }
    val ringAlpha     = remember { Animatable(0f) }
    val arcAlpha      = remember { Animatable(0f) }
    val arcProgress   = remember { Animatable(0f) }
    val iconScale     = remember { Animatable(0f) }
    val iconAlpha     = remember { Animatable(0f) }
    val titleAlpha    = remember { Animatable(0f) }
    val titleY        = remember { Animatable(20f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val barAlpha      = remember { Animatable(0f) }
    var loadStep      by remember { mutableStateOf(0) }
    val loadAlpha     = remember { Animatable(0f) }

    val inf = rememberInfiniteTransition(label = "inf")
    val rot by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "rot")
    val breathe by inf.animateFloat(1f, 1.04f,
        infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "br")
    val glowPulse by inf.animateFloat(0.75f, 1.25f,
        infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "gp")

    LaunchedEffect(Unit) {
        launch { glowAlpha.animateTo(1f, tween(600)) }
        delay(100)
        launch {
            ringAlpha.animateTo(1f, tween(400))
            ringScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
        delay(150); launch { arcAlpha.animateTo(1f, tween(350)) }
        delay(80)
        launch {
            iconAlpha.animateTo(1f, tween(250))
            iconScale.animateTo(1.1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
            iconScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
        delay(180)
        launch {
            titleAlpha.animateTo(1f, tween(300))
            titleY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
        delay(100); launch { subtitleAlpha.animateTo(1f, tween(260)) }
        delay(80);  launch { barAlpha.animateTo(1f, tween(240)) }
        delay(80);  launch { loadAlpha.animateTo(1f, tween(200)) }

        steps.forEachIndexed { i, _ ->
            loadStep = i
            arcProgress.animateTo((i + 1f) / steps.size, tween(280, easing = FastOutSlowInEasing))
            delay(320)
        }
        delay(280)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().background(surface), contentAlignment = Alignment.Center) {
        // glow
        Canvas(modifier = Modifier.fillMaxSize().alpha(glowAlpha.value)) {
            val cx = size.width / 2f; val cy = size.height * 0.38f
            val r = size.width * 0.7f * glowPulse
            drawCircle(
                brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(cx, cy), radius = r),
                radius = r, center = Offset(cx, cy)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-16).dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                // segmented ring
                Canvas(modifier = Modifier.fillMaxSize().scale(ringScale.value).alpha(ringAlpha.value)) {
                    val r = size.minDimension / 2f - 4.dp.toPx()
                    for (i in 0 until 8) {
                        drawArc(
                            color = primary.copy(alpha = if (i % 2 == 0) 0.55f else 0.18f),
                            startAngle = rot + i * 45f, sweepAngle = 30f, useCenter = false,
                            topLeft = Offset(size.width / 2 - r, size.height / 2 - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                // arc progress
                Canvas(modifier = Modifier.size(156.dp).alpha(arcAlpha.value)) {
                    val sw = 3.dp.toPx(); val pad = sw / 2 + 1.dp.toPx()
                    val sz = Size(size.width - pad * 2, size.height - pad * 2)
                    drawArc(color = primary.copy(alpha = 0.10f), startAngle = -90f, sweepAngle = 360f,
                        useCenter = false, topLeft = Offset(pad, pad), size = sz,
                        style = Stroke(sw, cap = StrokeCap.Round))
                    drawArc(brush = Brush.sweepGradient(listOf(primary.copy(alpha = 0.15f), primary, secondary)),
                        startAngle = -90f, sweepAngle = 360f * arcProgress.value,
                        useCenter = false, topLeft = Offset(pad, pad), size = sz,
                        style = Stroke(sw, cap = StrokeCap.Round))
                }
                // inner glow
                Canvas(modifier = Modifier.size(110.dp).alpha(ringAlpha.value)) {
                    drawCircle(brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.18f), Color.Transparent)))
                }
                // icon
                Box(modifier = Modifier.size(88.dp).scale(iconScale.value * breathe).alpha(iconAlpha.value).clip(CircleShape)) {
                    Image(painter = painterResource(R.mipmap.ic_launcher_round), contentDescription = null,
                        modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Aether Manager",
                modifier = Modifier.alpha(titleAlpha.value).offset(y = titleY.value.dp),
                color = onSurface, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp)

            Spacer(Modifier.height(4.dp))

            Text(s.splashSubtitle,
                modifier = Modifier.alpha(subtitleAlpha.value),
                color = primary.copy(alpha = 0.7f), fontSize = 12.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(40.dp))

            AnimatedLoadingText(steps = steps, current = loadStep,
                modifier = Modifier.alpha(loadAlpha.value), color = onVariant)

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.alpha(barAlpha.value).width(160.dp).height(2.dp)
                .clip(CircleShape).background(primary.copy(alpha = 0.10f))) {
                Box(modifier = Modifier.fillMaxWidth(arcProgress.value).fillMaxHeight()
                    .clip(CircleShape).background(
                        Brush.horizontalGradient(listOf(primary.copy(alpha = 0.4f), secondary))
                    ))
            }
        }

        Text("v1.4", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
            .alpha(subtitleAlpha.value * 0.4f),
            color = onVariant, fontSize = 10.sp, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun AnimatedLoadingText(steps: List<String>, current: Int, modifier: Modifier, color: Color) {
    val a = remember { Animatable(0f) }; val y = remember { Animatable(5f) }
    LaunchedEffect(current) {
        a.snapTo(0f); y.snapTo(5f)
        a.animateTo(1f, tween(170))
        y.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    Text(steps.getOrElse(current) { "" }, modifier = modifier.alpha(a.value).offset(y = y.value.dp),
        color = color, fontSize = 12.sp, letterSpacing = 0.2.sp)
}
