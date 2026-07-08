package com.aistudio.sepatify.ui.components

import android.media.audiofx.Equalizer
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistudio.sepatify.R

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

    val equalizer = remember {
        try {
            Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = dismissAction,
        confirmButton = {
            TextButton(onClick = dismissAction) {
                Text(text = stringResource(id = R.string.ok_label))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.drawer_equalizer_audio))
        },
        text = {
            if (equalizer != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val numBands = equalizer.numberOfBands.toInt()
                    val minLevel = equalizer.bandLevelRange[0]
                    val maxLevel = equalizer.bandLevelRange[1]

                    for (i in 0 until numBands) {
                        val freq = equalizer.getCenterFreq(i.toShort()) / 1000
                        var level by remember { mutableStateOf(equalizer.getBandLevel(i.toShort()).toFloat()) }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "$freq Hz")
                                Text(text = "${(level / 100).toInt()} dB")
                            }
                            Slider(
                                value = level,
                                onValueChange = { newValue ->
                                    level = newValue
                                    try {
                                        equalizer.setBandLevel(i.toShort(), newValue.toInt().toShort())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                valueRange = minLevel.toFloat()..maxLevel.toFloat()
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Audio Session not active or hardware not supported")
                }
            }
        }
    )
}