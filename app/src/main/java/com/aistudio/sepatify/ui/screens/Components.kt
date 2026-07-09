package com.aistudio.sepatify.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.model.Song
import coil.compose.AsyncImage

// ---------------------------------------------------------------------------
// Shimmer foundation
// ---------------------------------------------------------------------------

/**
 * Returns an animated left-to-right shimmer [Brush] to apply as a background.
 * All skeleton composables share this single brush so the sweep is in sync.
 */
@Composable
fun shimmerBrush(): Brush {
    val shimmerBase   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val shimmerHighlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f,
        targetValue  = 1400f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(shimmerBase, shimmerHighlight, shimmerBase),
        start  = Offset(x = translateX,        y = 0f),
        end    = Offset(x = translateX + 600f, y = 0f)
    )
}

/** Generic filled rectangle skeleton block — used as a building block by the typed skeletons below. */
@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    cornerRadius: Dp = 12.dp
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

// ---------------------------------------------------------------------------
// Typed skeleton composables  (one per card shape used in the app)
// ---------------------------------------------------------------------------

/**
 * Skeleton for a horizontal song row (cover thumbnail + two text lines).
 * Used in: Search results, Liked Songs, Recently Played, Playlist detail, Chat.
 */
@Composable
fun SongRowSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.40f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

/**
 * Skeleton for a vertical album card tile (110×110 image + two text lines below).
 * Used in: HomeScreen horizontal rows (Most Popular, New Releases, etc.).
 */
@Composable
fun SongCardSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier.width(110.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
    }
}

/**
 * Skeleton for a playlist grid card (matches the 134 dp bento card in PlaylistsScreen).
 */
@Composable
fun PlaylistCardSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(134.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brush)
    )
}

/**
 * Skeleton for a chat conversation row (avatar circle + two text lines).
 * Used in: ChatsScreen conversation list.
 */
@Composable
fun ChatRowSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(brush)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.50f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    avatarUrl: String,
    displayName: String,
    isPremium: Boolean,
    onSettingsClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    // TopBar according to NFR-13: "A common TopBar composable shall appear on all main screens with:
    // app logo and name (right), user avatar, notification icon, and settings icon (left)"
    // Since we handle both RTL and LTR automatically via Row,
    // placing settings, notifications, avatar on one side, and Logo and title on other side works beautifully.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT elements: Settings, Notifications, User Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = onNotificationsClick) {
                    BadgedBox(badge = { Badge { Text("3") } }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .clickable { onAvatarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initial = if (displayName.isNotEmpty()) displayName.take(1).uppercase() else "G"
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // RIGHT elements: App Logo and Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    currentSong: Song,
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onPlayerBarClick: () -> Unit,
    onDismiss: () -> Unit,
    coverModifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "miniPlayerOffset"
    )
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 140.dp.toPx() }

    LaunchedEffect(currentSong.id) {
        dragOffsetX = 0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .offset(x = with(density) { animatedOffsetX.toDp() })
            .pointerInput(currentSong.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-500f, 500f)
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(dragOffsetX) > dismissThresholdPx) {
                            onDismiss()
                        } else {
                            dragOffsetX = 0f
                        }
                    }
                )
            }
            .clickable {
                if (kotlin.math.abs(dragOffsetX) < 8f) {
                    onPlayerBarClick()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val art = rememberSongArt(currentSong)
                    AsyncImage(
                        model = art,
                        contentDescription = "Album Cover",
                        modifier = coverModifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = currentSong.artistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play_pause),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dismiss_mini_player),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Linear progress line
            val pct = if (duration > 0) progress.toFloat() / duration.toFloat() else 0f
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun rememberSongArt(song: Song): Any {
    var artModel by remember(song) { mutableStateOf<Any>(song.coverImageUrl) }
    LaunchedEffect(song) {
        if (song.id.startsWith("local_") || song.audioUrl.startsWith("/")) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(song.audioUrl)
                    val bytes = retriever.embeddedPicture
                    if (bytes != null) {
                        artModel = bytes
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
            }
        }
    }
    return artModel
}

fun extractLrcFile(filePath: String): List<LyricLine>? {
    val mp3File = java.io.File(filePath)
    val lrcFile = java.io.File(mp3File.parent, mp3File.nameWithoutExtension + ".lrc")
    if (lrcFile.exists()) {
        try {
            val lines = lrcFile.readLines()
            val lyricLines = mutableListOf<LyricLine>()
            val pattern = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
            for (line in lines) {
                val match = pattern.matchEntire(line.trim())
                if (match != null) {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val ms = match.groupValues[3].toLong() * 10
                    val text = match.groupValues[4].trim()
                    val timestamp = (min * 60 + sec) * 1000 + ms
                    lyricLines.add(LyricLine(timestamp, text))
                }
            }
            if (lyricLines.isNotEmpty()) {
                return lyricLines.sortedBy { it.timestampMs }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

fun extractLyricsFromMp3(filePath: String): List<LyricLine>? {
    // Try LRC file first
    val lrcLyrics = extractLrcFile(filePath)
    if (lrcLyrics != null) return lrcLyrics

    val file = java.io.File(filePath)
    if (!file.exists()) return null
    try {
        java.io.FileInputStream(file).use { fis ->
            val header = ByteArray(10)
            if (fis.read(header) != 10) return null
            if (header[0].toInt() != 'I'.toInt() || header[1].toInt() != 'D'.toInt() || header[2].toInt() != '3'.toInt()) {
                return null
            }
            val majorVersion = header[3].toInt()
            val s1 = header[6].toInt() and 0x7F
            val s2 = header[7].toInt() and 0x7F
            val s3 = header[8].toInt() and 0x7F
            val s4 = header[9].toInt() and 0x7F
            val tagSize = (s1 shl 21) or (s2 shl 14) or (s3 shl 7) or s4

            val tagData = ByteArray(tagSize)
            var totalRead = 0
            while (totalRead < tagSize) {
                val read = fis.read(tagData, totalRead, tagSize - totalRead)
                if (read == -1) break
                totalRead += read
            }

            var offset = 0
            while (offset < tagSize - 10) {
                if (tagData[offset] == 'U'.toByte() &&
                    tagData[offset + 1] == 'S'.toByte() &&
                    tagData[offset + 2] == 'L'.toByte() &&
                    tagData[offset + 3] == 'T'.toByte()
                ) {
                    val fSize = if (majorVersion == 4) {
                        val f1 = tagData[offset + 4].toInt() and 0x7F
                        val f2 = tagData[offset + 5].toInt() and 0x7F
                        val f3 = tagData[offset + 6].toInt() and 0x7F
                        val f4 = tagData[offset + 7].toInt() and 0x7F
                        (f1 shl 21) or (f2 shl 14) or (f3 shl 7) or f4
                    } else {
                        val f1 = tagData[offset + 4].toInt() and 0xFF
                        val f2 = tagData[offset + 5].toInt() and 0xFF
                        val f3 = tagData[offset + 6].toInt() and 0xFF
                        val f4 = tagData[offset + 7].toInt() and 0xFF
                        (f1 shl 24) or (f2 shl 16) or (f3 shl 8) or f4
                    }

                    if (fSize <= 0 || offset + 10 + fSize > tagSize) break

                    val frameDataOffset = offset + 10
                    val encoding = tagData[frameDataOffset].toInt()
                    var textOffset = frameDataOffset + 1 + 3

                    if (encoding == 0 || encoding == 3) {
                        while (textOffset < frameDataOffset + fSize && tagData[textOffset] != 0.toByte()) {
                            textOffset++
                        }
                        textOffset++
                    } else {
                        while (textOffset < frameDataOffset + fSize - 1 &&
                              !(tagData[textOffset] == 0.toByte() && tagData[textOffset + 1] == 0.toByte())) {
                            textOffset += 2
                        }
                        textOffset += 2
                    }

                    val textLen = (frameDataOffset + fSize) - textOffset
                    if (textLen > 0) {
                        val charset = when (encoding) {
                            1 -> Charsets.UTF_16
                            2 -> Charsets.UTF_16BE
                            3 -> Charsets.UTF_8
                            else -> Charsets.ISO_8859_1
                        }
                        val lyricsText = String(tagData, textOffset, textLen, charset).trim()
                        if (lyricsText.isNotEmpty()) {
                            val lines = lyricsText.split(Regex("\\r?\\n"))
                            return lines.mapIndexed { index, line ->
                                val timestamp = (index * 4000).toLong()
                                LyricLine(timestamp, line)
                            }
                        }
                    }
                    break
                }
                offset++
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
