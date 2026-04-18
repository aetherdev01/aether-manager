package dev.aether.manager.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.R
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.AppLanguage
import dev.aether.manager.i18n.LocalLanguage
import dev.aether.manager.i18n.LocalSetLanguage
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.home.TabSectionTitle

@Composable
fun AboutScreen(
    vm          : MainViewModel,
) {
    val s           = LocalStrings.current
    val ctx         = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Section: Developer ────────────────────────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Person,
            title = "Developer"
        )
        DevProfileCard()

        // ── Section: Komunitas & Tautan ───────────────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Language,
            title = "Community"
        )
        AboutSection {
            LinkRow(
                icon     = Icons.Outlined.Code,
                label    = "GitHub",
                subtitle = "github.com/aetherdev01",
                onClick  = {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aetherdev01"))
                    )
                }
            )
            AboutDivider()
            LinkRow(
                icon     = Icons.AutoMirrored.Outlined.Send,
                label    = "Telegram",
                subtitle = "@get01projects",
                onClick  = {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/get01projects"))
                    )
                }
            )
            AboutDivider()
            LinkRow(
                icon     = Icons.Outlined.Favorite,
                label    = "Saweria",
                subtitle = "saweria.co/AetherDev",
                onClick  = {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://saweria.co/AetherDev"))
                    )
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LANGUAGE DROPDOWN — no flag, text only
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutLanguageDropdown(modifier: Modifier = Modifier) {
    val currentLanguage = LocalLanguage.current
    val setLanguage     = LocalSetLanguage.current
    var expanded        by remember { mutableStateOf(false) }
    val arrowRotation   by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label         = "lang_arrow"
    )

    Box(modifier = modifier) {
        // Trigger chip
        Surface(
            shape         = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            onClick       = { expanded = true }
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Outlined.Language,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text      = currentLanguage.nativeName,
                    style     = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color     = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).rotate(arrowRotation),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Dropdown
        DropdownMenu(
            expanded          = expanded,
            onDismissRequest  = { expanded = false },
            offset            = DpOffset(0.dp, 4.dp),
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == currentLanguage
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text       = lang.nativeName,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text  = lang.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailingIcon = if (isSelected) ({
                        Icon(
                            Icons.Filled.Check, null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }) else null,
                    onClick = {
                        setLanguage(lang)
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            Color.Transparent
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEV PROFILE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DevProfileCard() {
    val s       = LocalStrings.current
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(primary.copy(alpha = 0.15f))
                )
                Box(
                    modifier         = Modifier.size(56.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter            = painterResource(id = R.drawable.profile_avatar),
                        contentDescription = "AetherDev",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "AetherDev",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(shape = CircleShape, color = primary) {
                        Box(Modifier.padding(3.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Check, null,
                                tint     = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
                Text(
                    "@AetherDev22",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Android & Root Module Developer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION WRAPPER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun AboutDivider() = HorizontalDivider(
    modifier  = Modifier.padding(start = 56.dp, end = 16.dp),
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    thickness = 0.5.dp
)

// ─────────────────────────────────────────────────────────────────────────────
// LINK ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LinkRow(
    icon    : ImageVector,
    label   : String,
    subtitle: String,
    onClick : () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew, null,
                tint     = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
