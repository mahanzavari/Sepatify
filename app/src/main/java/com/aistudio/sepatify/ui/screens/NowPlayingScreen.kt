package com.aistudio.sepatify.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.theme.sepatifyColors
import com.aistudio.sepatify.ui.theme.sepatifyDimens
import com.aistudio.sepatify.ui.theme.sepatifyShapes
import com.aistudio.sepatify.ui.viewmodel.DownloadViewModel
import com.aistudio.sepatify.ui.viewmodel.SharedAudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    sharedAudioViewModel: SharedAudioViewModel,
    downloadViewModel: DownloadViewModel,
    isPremium: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit = {},
    locString: (Int) -> String,
    coverModifier: Modifier = Modifier
) {
    val currentSong by sharedAudioViewModel.currentSong.collectAsState()
    val isPlaying by sharedAudioViewModel.isPlaying.collectAsState()
    val progress by sharedAudioViewModel.progress.collectAsState()
    val duration by sharedAudioViewModel.duration.collectAsState()
    val isShuffle by sharedAudioViewModel.isShuffle.collectAsState()
    val isRepeat by sharedAudioViewModel.isRepeat.collectAsState()
    val speed by sharedAudioViewModel.playbackSpeed.collectAsState()
    val sleepTimerMins by sharedAudioViewModel.sleepTimerMinutes.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val colors = MaterialTheme.sepatifyColors
    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes

    var dominantColor by remember { mutableStateOf(colors.playerBgFallback) }

    // DYNAMIC VINYL ROTATION ANGLE LINKED TO PLAYBACK SPEED
    var finalAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, speed) {
        if (isPlaying) {
            while (true) {
                val baseRotationSpeed = 1.2f
                finalAngle += (baseRotationSpeed * speed)

                if (finalAngle >= 360f) {
                    finalAngle -= 360f
                }
                kotlinx.coroutines.delay(16L)
            }
        }
    }

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "coverScale"
    )

    // EXTRACT COLOR USING PALETTE API
    LaunchedEffect(currentSong) {
        currentSong?.let { song ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    var bitmap: Bitmap? = null
                    if (song.id.startsWith("local_") || song.audioUrl.startsWith("/")) {
                        val retriever = android.media.MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(song.audioUrl)
                            val bytes = retriever.embeddedPicture
                            if (bytes != null) {
                                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            retriever.release()
                        }
                    }

                    if (bitmap == null) {
                        val url = URL(song.coverImageUrl)
                        val connection = url.openConnection()
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        val input = connection.getInputStream()
                        bitmap = BitmapFactory.decodeStream(input)
                    }

                    bitmap?.let { bmp ->
                        Palette.from(bmp).generate { palette ->
                            palette?.dominantSwatch?.rgb?.let { rgbColor ->
                                dominantColor = Color(rgbColor)
                            }
                        }
                    }
                } catch (e: Exception) {
                    dominantColor = colors.playerDefaultDominant
                }
            }
        }
    }

    // ALBUM COVER ROTATION VALUE
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val playPauseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "playPauseScale"
    )

    val shuffleScale by animateFloatAsState(
        targetValue = if (isShuffle) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "shuffleScale"
    )

    val repeatScale by animateFloatAsState(
        targetValue = if (isRepeat) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "repeatScale"
    )

    val formattedProgress = formatTime(progress)
    val formattedDuration = formatTime(duration)

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(text = locString(R.string.no_track_active))
        }
        return
    }

    val song = currentSong!!
    val isLiked by remember(song.id) {
        sharedAudioViewModel.isSongLiked(song.id)
    }.collectAsState(initial = false)
    val downloadedSongs by downloadViewModel.downloadedSongs.collectAsState()
    val isDownloaded = downloadedSongs.any { it.id == song.id }
    val downloadProgress by remember(song.id) {
        downloadViewModel.downloadProgressFor(song.id)
    }.collectAsState()
    var showUpgradeDialog by remember { mutableStateOf(false) }

    var parsedLyrics by remember(song) { mutableStateOf<List<LyricLine>?>(null) }
    LaunchedEffect(song) {
        if (song.id.startsWith("local_") || song.audioUrl.startsWith("/")) {
            withContext(Dispatchers.IO) {
                val extracted = extractLyricsFromMp3(song.audioUrl)
                if (extracted != null && extracted.isNotEmpty()) {
                    parsedLyrics = extracted
                } else {
                    parsedLyrics = getLyricsForSong(song.id, song.title)
                }
            }
        } else {
            parsedLyrics = getLyricsForSong(song.id, song.title)
        }
    }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = 180f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = dimens.alphaOverlayAmbient),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(dimens.spaceLarge)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffsetY = (dragOffsetY + dragAmount).coerceIn(0f, 1200f)
                        dragVelocity = dragAmount
                    },
                    onDragEnd = {
                        val shouldDismiss = dragOffsetY > dismissThresholdPx || dragVelocity > 480f
                        if (shouldDismiss) {
                            onBackClick()
                        } else {
                            dragOffsetY = 0f
                        }
                    }
                )
            }
            .graphicsLayer {
                translationY = dragOffsetY
                alpha = 1f - (dragOffsetY / 900f).coerceIn(0f, 0.35f)
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize player", tint = Color.White)
                }

                Text(
                    text = locString(R.string.now_playing_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val lyricsAvailable = !parsedLyrics.isNullOrEmpty()
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            if (lyricsAvailable) {
                                showLyrics = !showLyrics
                            } else {
                                android.widget.Toast.makeText(context, locString(R.string.no_lyrics_found), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (showLyrics) Icons.Filled.Lyrics else Icons.Outlined.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (!lyricsAvailable) Color.White.copy(alpha = dimens.alphaDisabled)
                                   else if (showLyrics) MaterialTheme.colorScheme.primary
                                   else Color.White
                        )
                    }
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerMins != null) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
                },
                modifier = Modifier
                    .weight(dimens.aspectRatioSquare)
                    .fillMaxWidth(),
                label = "lyrics_and_art_switcher"
            ) { targetShowLyrics ->
                if (targetShowLyrics) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = dimens.spaceFour),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimens.spaceEight, vertical = dimens.spaceFour),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                        ) {
                            Card(
                                modifier = Modifier.size(dimens.sizeAvatarMedium),
                                shape = RoundedCornerShape(dimens.spaceSix),
                                colors = CardDefaults.cardColors(containerColor = Color.Black)
                            ) {
                                val art = rememberSongArt(song)
                                AsyncImage(
                                    model = art,
                                    contentDescription = song.title,
                                    modifier = coverModifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(dimens.aspectRatioSquare)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = song.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = dimens.alphaSemiMuted),
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { sharedAudioViewModel.toggleLikeSong(song) }) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like Song",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(dimens.sizeIconLarge)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(dimens.aspectRatioSquare)
                                .fillMaxWidth()
                                .padding(top = dimens.spaceTwelve),
                            shape = shapes.card,
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = dimens.alphaShimmerBase)
                            ),
                            border = BorderStroke(dimens.borderThin, Color.White.copy(alpha = dimens.alphaShimmerHighlight))
                        ) {
                            val listState = rememberLazyListState()
                            val rawLyrics = parsedLyrics ?: emptyList()
                            val lyrics = remember(rawLyrics, duration) {
                                if (rawLyrics.isNotEmpty() && rawLyrics.last().timestampMs == ((rawLyrics.size - 1) * 4000).toLong() && duration > 0) {
                                    rawLyrics.mapIndexed { index, line ->
                                        LyricLine(
                                            timestampMs = (index * duration) / rawLyrics.size,
                                            text = line.text
                                        )
                                    }
                                } else {
                                    rawLyrics
                                }
                            }

                            val currentLineIndex = remember(lyrics, progress) {
                                val idx = lyrics.indexOfLast { progress >= it.timestampMs }
                                if (idx == -1) 0 else idx
                            }

                            LaunchedEffect(currentLineIndex) {
                                if (lyrics.isNotEmpty()) {
                                    listState.animateScrollToItem(
                                        index = currentLineIndex,
                                        scrollOffset = -180
                                    )
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = dimens.spaceNormal),
                                contentPadding = PaddingValues(vertical = dimens.lyricsPaddingVertical),
                                verticalArrangement = Arrangement.spacedBy(dimens.spaceTwenty),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(lyrics) { index, line ->
                                    val isSelected = index == currentLineIndex
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.05f else 0.95f,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                                        label = "lyricScale"
                                    )
                                    val alpha by animateFloatAsState(
                                        targetValue = if (isSelected) 1f else 0.45f,
                                        animationSpec = tween(300),
                                        label = "lyricAlpha"
                                    )

                                    Text(
                                        text = line.text,
                                        style = if (isSelected) {
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            MaterialTheme.typography.titleMedium.copy(
                                                color = Color.White
                                            )
                                        },
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                this.alpha = alpha
                                            }
                                            .clickable {
                                                sharedAudioViewModel.seekTo(line.timestampMs)
                                            }
                                            .padding(horizontal = dimens.spaceTwelve)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val targetDiskSize by animateDpAsState(
                                targetValue = if (isPlaying) dimens.sizeVinylDiskActive else dimens.sizeVinylDiskNormal,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "diskSize"
                            )

                            // Main vinyl record with animated smaller size
                            Box(
                                modifier = Modifier
                                    .size(targetDiskSize)
                                    .aspectRatio(dimens.aspectRatioSquare)
                                    .graphicsLayer {
                                        scaleX = coverScale
                                        scaleY = coverScale
                                        shadowElevation = dimens.sizeVinylDiskShadow.toPx()
                                        shape = CircleShape
                                        clip = true
                                    }
                                    .rotate(finalAngle)
                                    .background(Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // Vinyl grooves
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = this.center
                                    val maxRadius = size.minDimension / 2

                                    val maxRadiusInt = maxRadius.toInt()
                                    val minRadiusInt = (maxRadius * 0.45f).toInt()

                                    for (r in maxRadiusInt downTo minRadiusInt step dimens.spaceTwelve.value.toInt()) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = dimens.alphaGrooves),
                                            radius = r.toFloat(),
                                            center = center,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = dimens.borderThin.toPx())
                                        )
                                    }

                                    // Outer rim shine
                                    drawCircle(
                                        color = Color.White.copy(alpha = dimens.alphaRimShine),
                                        radius = maxRadius - dimens.sizeVinylDiskRimShine.toPx(),
                                        center = center,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = dimens.borderHeavy.toPx())
                                    )
                                }

                                // Center album art sticker
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(dimens.scaleSticker)
                                        .aspectRatio(dimens.aspectRatioSquare)
                                        .clip(CircleShape)
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val art = rememberSongArt(song)
                                    AsyncImage(
                                        model = art,
                                        contentDescription = song.title,
                                        modifier = coverModifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Center spindle hole
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(dimens.scaleSpindleHole)
                                        .aspectRatio(dimens.aspectRatioSquare)
                                        .background(colors.playerSpindleHole, CircleShape)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceTwelve),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(dimens.aspectRatioSquare),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = song.artistName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = dimens.alphaStandard),
                                    maxLines = 1
                                )
                            }
                            val likeScale by animateFloatAsState(
                                targetValue = if (isLiked) 1.2f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                label = "likeScale"
                            )
                            IconButton(
                                onClick = { sharedAudioViewModel.toggleLikeSong(song) },
                                modifier = Modifier.graphicsLayer {
                                    scaleX = likeScale
                                    scaleY = likeScale
                                }
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like Song",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(dimens.sizeIconPlayPauseCircle)
                                )
                            }

                            IconButton(onClick = onShareClick) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(dimens.sizeIconControl)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (!isPremium) {
                                        showUpgradeDialog = true
                                    } else if (!isDownloaded && downloadProgress == null) {
                                        downloadViewModel.initiateDownload(song)
                                    }
                                }
                            ) {
                                if (downloadProgress != null) {
                                    CircularProgressIndicator(
                                        progress = { downloadProgress ?: 0f },
                                        modifier = Modifier.size(dimens.sizeIconNormal),
                                        color = Color.White,
                                        strokeWidth = dimens.borderThick
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                        contentDescription = locString(R.string.download_song),
                                        tint = if (isDownloaded) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(dimens.sizeIconControl)
                                    )
                                }
                            }
                        }

                        if (showUpgradeDialog) {
                            AlertDialog(
                                onDismissRequest = { showUpgradeDialog = false },
                                title = { Text(locString(R.string.upgrade_required_title)) },
                                text = { Text(locString(R.string.download_requires_premium)) },
                                confirmButton = {
                                    TextButton(onClick = { showUpgradeDialog = false }) {
                                        Text(locString(R.string.ok_label))
                                    }
                                }
                            )
                        }

                        AudioVisualizerComponent(
                            sharedAudioViewModel = sharedAudioViewModel,
                            progress = progress,
                            duration = duration
                        )
                    }
                }
            }

            // Seek Position timeline sliders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimens.spaceTwelve),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSix)
            ) {
                val progressPct = if (duration > 0) progress.toFloat() / duration.toFloat() else 0f
                val sliderInteractionSource = remember { MutableInteractionSource() }
                val isSliderDragged by sliderInteractionSource.collectIsDraggedAsState()
                val isSliderPressed by sliderInteractionSource.collectIsPressedAsState()
                val isSliderInteracting = isSliderDragged || isSliderPressed

                val trackHeight by animateDpAsState(
                    targetValue = if (isSliderInteracting) dimens.heightSliderTrackExpanded else dimens.heightSliderTrackDefault,
                    animationSpec = tween(durationMillis = 150),
                    label = "trackHeight"
                )

                val thumbScale by animateFloatAsState(
                    targetValue = if (isSliderInteracting) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "thumbScale"
                )

                Slider(
                    value = progressPct,
                    onValueChange = { sharedAudioViewModel.seekTo((it * duration).toLong()) },
                    interactionSource = sliderInteractionSource,
                    track = { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimens.heightSliderTrackWrapper),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight)
                                    .background(Color.White.copy(alpha = dimens.alphaShimmerHighlight), CircleShape),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = progressPct)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    },
                    thumb = {
                        Box(
                            modifier = Modifier
                                .height(dimens.heightSliderTrackWrapper)
                                .width(dimens.heightSliderTrackWrapper),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dimens.sizeSliderThumb)
                                    .graphicsLayer {
                                        scaleX = thumbScale
                                        scaleY = thumbScale
                                    }
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spaceTwo),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formattedProgress,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSliderInteracting) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = dimens.alphaMuted)
                    )
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = dimens.alphaMuted)
                    )
                }
            }

            // CONTROLS INTERACTIVE ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimens.spaceNormal),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { sharedAudioViewModel.toggleShuffle() },
                    modifier = Modifier.graphicsLayer {
                        scaleX = shuffleScale
                        scaleY = shuffleScale
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White
                    )
                }

                IconButton(onClick = { sharedAudioViewModel.playPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(dimens.sizeIconSkip)
                    )
                }

                IconButton(
                    onClick = { sharedAudioViewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(dimens.sizePlayPauseContainer)
                        .graphicsLayer {
                            scaleX = playPauseScale
                            scaleY = playPauseScale
                        }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(dimens.sizeIconPlayPause)
                    )
                }

                IconButton(onClick = { sharedAudioViewModel.playNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(dimens.sizeIconSkip)
                    )
                }

                IconButton(
                    onClick = { sharedAudioViewModel.toggleRepeat() },
                    modifier = Modifier.graphicsLayer {
                        scaleX = repeatScale
                        scaleY = repeatScale
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }

            // PLAYBACK SPEED ADJUSTMENT FOOTER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimens.spaceTwelve),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val speedText = when (speed) {
                    1.5f -> "1.5x"
                    2.0f -> "2.0x"
                    else -> "1.0x"
                }

                var buttonClicked by remember { mutableStateOf(false) }
                val speedButtonScale by animateFloatAsState(
                    targetValue = if (buttonClicked) 0.9f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    finishedListener = { buttonClicked = false },
                    label = "speedButtonScale"
                )

                Button(
                    onClick = {
                        buttonClicked = true
                        val nextSpeed = when (speed) {
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        sharedAudioViewModel.setPlaybackSpeed(nextSpeed)
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = speedButtonScale
                            scaleY = speedButtonScale
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = shapes.small,
                    border = BorderStroke(dimens.borderThin, MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaMuted))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSix)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            modifier = Modifier.size(dimens.sizeIconSpeed)
                        )
                        AnimatedContent(
                            targetState = speedText,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "speedTextTransition"
                        ) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // SLEEP TIMER PROMPT DIALOG
        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text(locString(R.string.sleep_timer)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceTen)) {
                        Text(text = locString(R.string.sleep_timer_inactive))
                        listOf(5, 15, 30, 45, 60).forEach { mins ->
                            Button(
                                onClick = {
                                    sharedAudioViewModel.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = locString(R.string.sleep_timer_minutes_val).replace("%1\$d", mins.toString()),
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                        if (sleepTimerMins != null) {
                            Button(
                                onClick = {
                                    sharedAudioViewModel.setSleepTimer(null)
                                    showSleepTimerDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(text = locString(R.string.sleep_timer_off), color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 1000) / 60
    return "%02d:%02d".format(min, sec)
}

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

fun getLyricsForSong(songId: String, songTitle: String): List<LyricLine> {
    return when (songId) {
        "33" -> listOf(
            LyricLine(0, "(Soft pencil scratching and coffee sipping)"),
            LyricLine(8000, "Focus deep, the clock is ticking slow..."),
            LyricLine(15000, "Flipping pages in the amber glow..."),
            LyricLine(22000, "No distractions, just the quiet mind..."),
            LyricLine(30000, "In these books, a peace we hope to find..."),
            LyricLine(38000, "Pencil clicks and thoughts begin to flow..."),
            LyricLine(46000, "Take a breath, let your ideas grow..."),
            LyricLine(54000, "(Gentle keyboard typing sounds)"),
            LyricLine(65000, "Late night hours, the world is fast asleep..."),
            LyricLine(74000, "Promises of dawn we intend to keep..."),
            LyricLine(82000, "Step by step, the puzzle pieces fit..."),
            LyricLine(90000, "A spark of light, completely lit..."),
            LyricLine(100000, "Focus deep, the session comes to close..."),
            LyricLine(110000, "And that's how the knowledge grows...")
        )
        "34" -> listOf(
            LyricLine(0, "(Train clacking on the tracks, distant horn)"),
            LyricLine(10000, "Midnight train, heading out to the west..."),
            LyricLine(18000, "City lights fading, it's time to rest..."),
            LyricLine(25000, "Passengers staring out at the rain..."),
            LyricLine(32000, "Nostalgic memories of love and pain..."),
            LyricLine(40000, "Click-clack, the rhythm of the rail..."),
            LyricLine(48000, "A silent journey on an endless trail..."),
            LyricLine(56000, "Do you remember the words we used to say?"),
            LyricLine(64000, "Before we let our dreams slip away..."),
            LyricLine(72000, "Now the train rolls on into the night..."),
            LyricLine(80000, "Waiting for the morning first light...")
        )
        "35" -> listOf(
            LyricLine(0, "(Crackling embers and acoustic guitar intro)"),
            LyricLine(12000, "Warm embers dancing in the canyon breeze..."),
            LyricLine(22000, "A simple melody between the pine trees..."),
            LyricLine(32000, "The guitar strings speak of ancient days..."),
            LyricLine(42000, "Underneath the milky way's soft haze..."),
            LyricLine(52000, "Fireside glow, a comforting embrace..."),
            LyricLine(62000, "In this quiet canyon, we find our space..."),
            LyricLine(72000, "No rushing rivers, no city noise..."),
            LyricLine(82000, "Just the simple chords and natural poise..."),
            LyricLine(92000, "(Slow melodic guitar solo)")
        )
        "1", "2", "3", "4", "5", "6", "7", "8" -> listOf(
            LyricLine(0, "(Intro Instrumental)"),
            LyricLine(5000, "Hey, welcome to the beautiful rhythm of $songTitle..."),
            LyricLine(12000, "Feel the vibe, let it sink deep within..."),
            LyricLine(18000, "Let the music take absolute control..."),
            LyricLine(24000, "Let it heal your restless mind and soul..."),
            LyricLine(32000, "Yeah, we are gliding through the cosmic night..."),
            LyricLine(38000, "Everything is gonna be alright..."),
            LyricLine(45000, "Enjoy this serene soundscape...")
        )
        else -> emptyList()
    }
}

@Composable
fun AudioVisualizerComponent(
    sharedAudioViewModel: SharedAudioViewModel,
    progress: Long,
    duration: Long
) {
    val rawVisualizerBars by sharedAudioViewModel.fftBands.collectAsState()
    val isBassDetected by sharedAudioViewModel.isBassDetected.collectAsState()

    val totalBarsCount = 12
    val density = LocalDensity.current
    val dimens = MaterialTheme.sepatifyDimens

    // Local smoothing state to prevent jittery movements
    var smoothedBars by remember { mutableStateOf(FloatArray(totalBarsCount) { 0f }) }

    LaunchedEffect(rawVisualizerBars) {
        val nextBars = FloatArray(totalBarsCount)
        for (i in 0 until totalBarsCount) {
            val target = if (i < rawVisualizerBars.size) rawVisualizerBars[i] else 0f
            val current = smoothedBars[i]
            nextBars[i] = current + 0.7f * (target - current)
        }
        smoothedBars = nextBars
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(dimens.alphaOverlayAmbient)
            .height(dimens.heightVisualizerContainer),
        contentAlignment = Alignment.Center
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary

        val crossfadeVolumeFactor = remember(progress, duration) {
            val currentMs = progress
            val totalMs = duration
            val crossfadeDurationMs = 4000L

            when {
                totalMs <= 0 -> 1f
                currentMs < crossfadeDurationMs -> {
                    val ratio = (currentMs.toFloat() / crossfadeDurationMs).coerceIn(0f, 1f)
                    ratio * ratio
                }
                currentMs > (totalMs - crossfadeDurationMs) -> {
                    val remainingMs = totalMs - currentMs
                    val ratio = (remainingMs.toFloat() / crossfadeDurationMs).coerceIn(0f, 1f)
                    ratio * ratio
                }
                else -> 1f
            }
        }

        val rawSubBass = remember(smoothedBars, crossfadeVolumeFactor) {
            if (smoothedBars.isEmpty()) 0f else {
                val sampleCount = smoothedBars.size.coerceAtMost(2)
                var sum = 0f
                for (i in 0 until sampleCount) {
                    sum += smoothedBars[i]
                }
                ((sum / sampleCount) * 1.8f) * crossfadeVolumeFactor
            }
        }

        val animatedSubBassScale by animateFloatAsState(
            targetValue = (rawSubBass / 10f).coerceIn(0f, 2.5f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "SubBassScale"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalBars = smoothedBars.size
            if (totalBars == 0) return@Canvas

            val gapFraction = dimens.alphaMuted
            val availableWidth = size.width

            val barWidth = (availableWidth * (1f - gapFraction)) / totalBars
            val gap = (availableWidth * gapFraction) / (totalBars - 1).coerceAtLeast(1)

            for (i in 0 until totalBars) {
                val rawBarValue = smoothedBars[i]
                val positionFactor = i.toFloat() / totalBars

                val personalScale = if (positionFactor < 0.35f) {
                    animatedSubBassScale * 1.1f
                } else if (positionFactor < 0.75f) {
                    1.2f * crossfadeVolumeFactor
                } else {
                    1.4f * crossfadeVolumeFactor
                }

                val rawHeight = (rawBarValue.dp.toPx() * personalScale) * crossfadeVolumeFactor
                val minHeight = (dimens.visualizerMinHeight.toPx() * crossfadeVolumeFactor).coerceAtLeast(0f)
                val barHeight = rawHeight.coerceIn(minHeight, size.height)

                val x = i * (barWidth + gap)
                val y = size.height - barHeight

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}