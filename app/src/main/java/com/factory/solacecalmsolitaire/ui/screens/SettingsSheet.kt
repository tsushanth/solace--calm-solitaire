package com.factory.solacecalmsolitaire.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.factory.solacecalmsolitaire.R
import com.factory.solacecalmsolitaire.ui.components.LockIcon
import com.factory.solacecalmsolitaire.ui.components.ProBadge
import com.factory.solacecalmsolitaire.ui.theme.FeltTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    drawCount: Int,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    autoCompleteEnabled: Boolean,
    isPremium: Boolean,
    feltTheme: FeltTheme,
    dynamicColorEnabled: Boolean,
    onDrawCountChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onAutoCompleteChange: (Boolean) -> Unit,
    onFeltThemeChange: (FeltTheme) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    fun tapFeedback() {
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            PremiumStatusRow(isPremium = isPremium, onUpgrade = { tapFeedback(); onUpgrade() })

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(stringResource(R.string.settings_draw_count), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                val drawOneLocked = !isPremium
                val drawOneLabel = stringResource(R.string.settings_draw_one)
                val drawOneDescription = if (drawOneLocked) {
                    "$drawOneLabel. ${stringResource(R.string.cd_locked_premium)}"
                } else {
                    drawOneLabel
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = drawCount == 1,
                            onClick = { tapFeedback(); onDrawCountChange(1) },
                            role = Role.RadioButton
                        )
                        .semantics { contentDescription = drawOneDescription },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = drawCount == 1, onClick = null)
                    Text(drawOneLabel)
                    if (drawOneLocked) LockIcon()
                }
                val drawThreeLabel = stringResource(R.string.settings_draw_three)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = drawCount == 3,
                            onClick = { tapFeedback(); onDrawCountChange(3) },
                            role = Role.RadioButton
                        )
                        .semantics { contentDescription = drawThreeLabel },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = drawCount == 3, onClick = null)
                    Text(drawThreeLabel)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SettingsToggleRow(
                label = stringResource(R.string.settings_sound),
                checked = soundEnabled,
                onChange = { onSoundChange(it); tapFeedback() }
            )
            SettingsToggleRow(
                label = stringResource(R.string.settings_haptics),
                checked = hapticsEnabled,
                onChange = { onHapticsChange(it); if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            )
            PremiumToggleRow(
                label = stringResource(R.string.settings_auto_complete),
                checked = autoCompleteEnabled,
                isPremium = isPremium,
                onChange = { onAutoCompleteChange(it); tapFeedback() },
                onUpgrade = { tapFeedback(); onUpgrade() }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsToggleRow(
                    label = stringResource(R.string.settings_dynamic_color),
                    checked = dynamicColorEnabled,
                    onChange = { onDynamicColorChange(it); tapFeedback() }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            FeltThemeSection(
                selected = feltTheme,
                isPremium = isPremium,
                onSelect = { tapFeedback(); onFeltThemeChange(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PremiumStatusRow(isPremium: Boolean, onUpgrade: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isPremium) "Solace Premium" else "Free plan",
                style = MaterialTheme.typography.titleMedium
            )
        }
        TextButton(onClick = onUpgrade) {
            Text(if (isPremium) "Manage" else "Upgrade")
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onChange, role = Role.Switch)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PremiumToggleRow(
    label: String,
    checked: Boolean,
    isPremium: Boolean,
    onChange: (Boolean) -> Unit,
    onUpgrade: () -> Unit
) {
    val lockedDescription = stringResource(R.string.cd_locked_premium)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPremium) {
                    Modifier.toggleable(value = checked, onValueChange = onChange, role = Role.Switch)
                } else {
                    Modifier
                        .clickable(onClick = onUpgrade)
                        .semantics { contentDescription = "$label. $lockedDescription" }
                }
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (!isPremium) {
                Spacer(modifier = Modifier.width(8.dp))
                ProBadge()
            }
        }
        Switch(checked = isPremium && checked, onCheckedChange = null, enabled = isPremium)
    }
}

@Composable
private fun FeltThemeSection(selected: FeltTheme, isPremium: Boolean, onSelect: (FeltTheme) -> Unit) {
    Text(stringResource(R.string.settings_felt_theme), style = MaterialTheme.typography.titleMedium)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FeltTheme.entries.forEach { theme ->
            val locked = theme.isPremium && !isPremium
            val lockedDescription = stringResource(R.string.cd_locked_premium)
            val themeDescription = stringResource(R.string.cd_felt_theme_option, theme.label) +
                if (locked) ". $lockedDescription" else ""
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .selectable(selected = theme == selected, onClick = { onSelect(theme) }, role = Role.RadioButton)
                    .semantics(mergeDescendants = true) { contentDescription = themeDescription }
            ) {
                Row(
                    modifier = Modifier
                        .size(44.dp)
                        .background(theme.color, CircleShape)
                        .border(
                            width = if (theme == selected) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (theme == selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    } else if (locked) {
                        LockIcon(modifier = Modifier.padding(0.dp), tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(theme.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
