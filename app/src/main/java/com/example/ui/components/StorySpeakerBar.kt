package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.StorySpeakerManager
import com.example.audio.WomanVoiceStyle
import com.example.ui.theme.*

/**
 * Compact, unified bottom reader bar with:
 * - Previous Page Button
 * - Central Play / Speak (Woman Voice) button + mini voice status & settings
 * - Next Page Button
 * Designed to minimize vertical footprint so the storybook page gets maximum screen space.
 */
@Composable
fun CompactReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    speakerManager: StorySpeakerManager,
    modifier: Modifier = Modifier
) {
    val currentStyle by speakerManager.currentStyle.collectAsState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1E293B),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- PREVIOUS BUTTON ---
            FilledTonalButton(
                onClick = onPrevious,
                enabled = currentPage > 0,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF334155),
                    disabledContainerColor = Color(0x33334155)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Page",
                    tint = if (currentPage > 0) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prev", fontSize = 12.sp, color = if (currentPage > 0) Color.White else Color(0xFF64748B))
            }

            // --- CENTER: COMPACT AUDIO PLAY & WOMAN VOICE OPTION ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play / Pause Speak Button
                FilledIconButton(
                    onClick = onToggleSpeak,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isSpeaking) SparkPrimary else SparkSecondary
                    )
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isSpeaking) "Pause Story Narration" else "Play Story Narration in Woman Voice",
                        tint = if (isSpeaking) Color.White else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Voice status & quick settings trigger
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenVoiceSettings)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSpeaking) "Playing • ${currentStyle.displayName.substringBefore(" ")}" else "Play (Woman Voice)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (isSpeaking) {
                            SpeakingWaveform()
                        } else {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = SparkSecondary
                            ) {
                                Text(
                                    text = "PLAY",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isSpeaking) "Tap pause to stop • ${currentPage + 1} of $totalPages" else "${currentPage + 1} of $totalPages • Voice settings",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            // --- NEXT BUTTON ---
            Button(
                onClick = onNext,
                enabled = currentPage < totalPages - 1,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SparkPrimary,
                    disabledContainerColor = Color(0x33334155)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Next", fontSize = 12.sp, color = if (currentPage < totalPages - 1) Color.White else Color(0xFF64748B))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Page",
                    tint = if (currentPage < totalPages - 1) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Modal dialog for woman voice settings (style, speed, auto-flip).
 * Does not take up any screen space until opened by user!
 */
@Composable
fun WomanVoiceSettingsDialog(
    speakerManager: StorySpeakerManager,
    autoAdvance: Boolean,
    onToggleAutoAdvance: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val currentStyle by speakerManager.currentStyle.collectAsState()
    val speedMultiplier by speakerManager.speechRateMultiplier.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SparkPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Woman Voice Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Storyteller Tone & Pacing", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose Female Voice Persona:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                WomanVoiceStyle.values().forEach { style ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { speakerManager.setVoiceStyle(style) },
                        color = if (currentStyle == style) SparkPrimaryContainer.copy(alpha = 0.5f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentStyle == style,
                                onClick = { speakerManager.setVoiceStyle(style) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(style.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(style.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                Text("Reading Speed:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.8f to "0.8x Bedtime", 1.0f to "1.0x Normal", 1.2f to "1.2x Fast").forEach { (speed, label) ->
                        FilterChip(
                            selected = speedMultiplier == speed,
                            onClick = { speakerManager.setRateMultiplier(speed) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoAdvance,
                        onCheckedChange = onToggleAutoAdvance
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-flip to next page when finished speaking", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
fun SpeakingWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 11f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h1.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(SparkSecondary)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(SparkSecondary)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h3.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(SparkSecondary)
        )
    }
}
