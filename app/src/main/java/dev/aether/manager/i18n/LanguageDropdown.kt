package dev.aether.manager.i18n

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A dropdown button that lets the user pick an [AppLanguage].
 *
 * Reads current language from [LocalLanguage] and changes it via [LocalSetLanguage].
 * No parameters required — just drop it anywhere in the composable tree inside [ProvideStrings].
 *
 * Example:
 * ```kotlin
 * // In AboutScreen or Settings
 * LanguageDropdown()
 * ```
 */
@Composable
fun LanguageDropdown(
    modifier: Modifier = Modifier,
) {
    val currentLanguage = LocalLanguage.current
    val setLanguage = LocalSetLanguage.current

    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "arrow"
    )

    Box(modifier = modifier) {
        // ── Trigger button ────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = currentLanguage.flag,
                    fontSize = 18.sp,
                )
                Text(
                    text = currentLanguage.nativeName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select language",
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(arrowRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Dropdown menu ─────────────────────────────────────────────────────
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 4.dp),
            modifier = Modifier
                .widthIn(min = 200.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == currentLanguage

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = lang.flag,
                                fontSize = 20.sp,
                            )
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        setLanguage(lang)
                        expanded = false
                    },
                    trailingIcon = if (isSelected) ({
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown, // checkmark placeholder
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(180f), // flip = up = "selected" hint
                        )
                    }) else null,
                    modifier = Modifier.background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            Color.Transparent
                    ),
                )
            }
        }
    }
}

/**
 * A compact icon-only language button (shows flag + arrow).
 * Useful in TopAppBar trailing actions.
 */
@Composable
fun LanguageDropdownCompact(
    modifier: Modifier = Modifier,
) {
    val currentLanguage = LocalLanguage.current
    val setLanguage = LocalSetLanguage.current

    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Text(
                text = currentLanguage.flag,
                fontSize = 22.sp,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 0.dp),
            modifier = Modifier
                .widthIn(min = 180.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == currentLanguage
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(text = lang.flag, fontSize = 18.sp)
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    onClick = {
                        setLanguage(lang)
                        expanded = false
                    },
                )
            }
        }
    }
}
