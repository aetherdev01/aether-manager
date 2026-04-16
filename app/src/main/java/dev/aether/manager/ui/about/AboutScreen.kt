package dev.aether.manager.ui.about

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.R
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.home.TabSectionTitle

@Composable
fun AboutScreen(vm: MainViewModel) {
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
        // ── Section: Developer ────────────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Person,
            title = s.aboutSectionDev
        )
        DevProfileCard()

        // ── Section: App Info ─────────────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Info,
            title = s.aboutSectionAppInfo
        )
        AboutSection {
            AboutRow(s.aboutApp,     "Aether Manager",              Icons.Outlined.Extension,   MaterialTheme.colorScheme.primary)
            AboutDivider()
            AboutRow(s.aboutVersion, "v2.0 (2)",                    Icons.Outlined.Tag,          MaterialTheme.colorScheme.secondary)
            AboutDivider()
            AboutRow(s.aboutMode,    s.aboutModeValue,              Icons.Outlined.Folder,       MaterialTheme.colorScheme.tertiary)
            AboutDivider()
            AboutRow(s.aboutSupport, "MTK · SD · Exynos · Kirin",   Icons.Outlined.Devices,      MaterialTheme.colorScheme.primary)
        }

        // ── Section: Komunitas ────────────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Language,
            title = s.aboutSectionLinks
        )
        AboutSection {
            LinkRow(
                icon           = Icons.Outlined.Code,
                label          = s.aboutGithub,
                subtitle       = "github.com/aetherdev01",
                badge          = "Open Source",
                badgeColor     = MaterialTheme.colorScheme.primaryContainer,
                badgeTextColor = MaterialTheme.colorScheme.primary,
                onClick        = { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/aetherdev01"))) }
            )
            AboutDivider()
            LinkRow(
                icon           = Icons.Outlined.Send,
                label          = s.aboutTelegram,
                subtitle       = "@get01projects",
                badge          = "Channel",
                badgeColor     = MaterialTheme.colorScheme.secondaryContainer,
                badgeTextColor = MaterialTheme.colorScheme.secondary,
                onClick        = { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/get01projects"))) }
            )
            AboutDivider()
            LinkRow(
                icon           = Icons.Outlined.Favorite,
                label          = s.aboutSaweriaLabel,
                subtitle       = s.aboutSaweria,
                badge          = "Support",
                badgeColor     = MaterialTheme.colorScheme.tertiaryContainer,
                badgeTextColor = MaterialTheme.colorScheme.tertiary,
                onClick        = { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://saweria.co/AetherDev"))) }
            )
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
                // Name + verified badge
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
                    s.aboutDevDesc,
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
// ABOUT ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutRow(
    key: String, value: String,
    icon: ImageVector, iconTint: Color
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(17.dp))
        }
        Text(
            key,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        Text(
            value,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LINK ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LinkRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color,
    onClick: () -> Unit
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
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        label,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(shape = CircleShape, color = badgeColor) {
                        Text(
                            badge,
                            modifier  = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize  = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color     = badgeTextColor
                        )
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Outlined.OpenInNew, null,
                tint     = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
