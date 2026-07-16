package com.aistudio.sepatify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.sepatify.R
import com.aistudio.sepatify.ui.viewmodel.SharedAudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerDialog(
    audioSessionId: Int = 0,
    onDismissRequest: () -> Unit = {},
    viewModel: Any? = null,
    onDismiss: () -> Unit = {}
) {
    val dismissAction = {
        onDismissRequest()
        onDismiss()
    }

    val sharedViewModel = viewModel as? SharedAudioViewModel

    if (sharedViewModel == null) {
        AlertDialog(
            onDismissRequest = dismissAction,
            confirmButton = {
                TextButton(onClick = dismissAction) {
                    Text(text = stringResource(id = R.string.ok_label))
                }
            },
            text = { Text("Audio session/viewmodel not available") }
        )
        return
    }

    val eqEnabled by sharedViewModel.eqEnabled.collectAsState()
    val eqBandLevels by sharedViewModel.eqBandLevels.collectAsState()
    val eqFrequencies by sharedViewModel.eqFrequencies.collectAsState()
    val eqBandRange by sharedViewModel.eqBandRange.collectAsState()
    val bassBoostStrength by sharedViewModel.bassBoostStrength.collectAsState()
    val virtualizerStrength by sharedViewModel.virtualizerStrength.collectAsState()
    val reverbPreset by sharedViewModel.reverbPreset.collectAsState()
    val crossfadeEnabled by sharedViewModel.crossfadeEnabled.collectAsState()
    val crossfadeDurationSec by sharedViewModel.crossfadeDurationSec.collectAsState()

    AlertDialog(
        onDismissRequest = dismissAction,
        confirmButton = {
            TextButton(onClick = dismissAction) {
                Text(text = stringResource(id = R.string.ok_label))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.drawer_equalizer_audio), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // === Playback Crossfade ===
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.playback_crossfade), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.playback_crossfade_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = crossfadeEnabled,
                            onCheckedChange = { sharedViewModel.setCrossfadeEnabled(it) }
                        )
                    }

                    if (crossfadeEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.transition_duration), style = MaterialTheme.typography.bodyMedium)
                                Text(text = stringResource(R.string.seconds_value, crossfadeDurationSec), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = crossfadeDurationSec.toFloat(),
                                onValueChange = { sharedViewModel.setCrossfadeDuration(it.toInt()) },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // === Hardware Tuning Switch ===
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.equalizer_effects), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.equalizer_effects_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { sharedViewModel.setEqualizerEnabled(it) }
                        )
                    }
                }

                if (eqEnabled) {
                    // === Equalizer Bands ===
                    if (eqFrequencies.isNotEmpty()) {
                        Text(text = stringResource(R.string.bands), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        val minLevel = eqBandRange.first
                        val maxLevel = eqBandRange.second

                        eqFrequencies.forEachIndexed { index, freq ->
                            val levelMilliBels = eqBandLevels[index] ?: 0
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.frequency_hz, freq), style = MaterialTheme.typography.bodyMedium)
                                    Text(text = stringResource(R.string.db_value, levelMilliBels / 100), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = levelMilliBels.toFloat(),
                                    onValueChange = { newValue ->
                                        sharedViewModel.setEqualizerBandLevel(index, newValue.toInt())
                                    },
                                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // === Bass & Virtualizer ===
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = stringResource(R.string.audio_enhancements), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        // Bass Boost
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.bass_boost), style = MaterialTheme.typography.bodyMedium)
                                Text(text = stringResource(R.string.percent_value, bassBoostStrength / 10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = bassBoostStrength.toFloat(),
                                onValueChange = { sharedViewModel.setBassBoostStrength(it.toInt()) },
                                valueRange = 0f..1000f
                            )
                        }

                        // Virtualizer
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.virtualizer_3d), style = MaterialTheme.typography.bodyMedium)
                                Text(text = stringResource(R.string.percent_value, virtualizerStrength / 10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = virtualizerStrength.toFloat(),
                                onValueChange = { sharedViewModel.setVirtualizerStrength(it.toInt()) },
                                valueRange = 0f..1000f
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // === Reverb Preset ===
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.reverb_preset), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        val presets = listOf(
                            stringResource(R.string.reverb_none),
                            stringResource(R.string.reverb_small_room),
                            stringResource(R.string.reverb_medium_room),
                            stringResource(R.string.reverb_large_room),
                            stringResource(R.string.reverb_medium_hall),
                            stringResource(R.string.reverb_large_hall),
                            stringResource(R.string.reverb_plate)
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = presets.getOrElse(reverbPreset) { stringResource(R.string.reverb_none) })
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                presets.forEachIndexed { idx, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            sharedViewModel.setReverbPreset(idx)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}