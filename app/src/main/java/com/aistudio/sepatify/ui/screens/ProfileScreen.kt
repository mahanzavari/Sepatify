package com.aistudio.sepatify.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.ui.theme.sepatifyColors
import com.aistudio.sepatify.ui.theme.sepatifyDimens
import com.aistudio.sepatify.ui.theme.sepatifyShapes
import com.aistudio.sepatify.ui.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileScreen(
    mainViewModel: MainViewModel,
    locString: (Int) -> String
) {
    val context = LocalContext.current
    val displayName by mainViewModel.userDisplayName.collectAsState()
    val email by mainViewModel.userEmail.collectAsState()
    val avatarUrl by mainViewModel.userAvatar.collectAsState()
    val isPremium by mainViewModel.isPremium.collectAsState()
    val currentTheme by mainViewModel.currentTheme.collectAsState()
    val currentLang by mainViewModel.currentLanguage.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    val colors = MaterialTheme.sepatifyColors
    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            showCropDialog = true
        }
    }

    if (showCropDialog && selectedImageUri != null) {
        Dialog(onDismissRequest = { showCropDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(dimens.spaceNormal),
                shape = shapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(dimens.spaceLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = locString(R.string.crop_dialog_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(dimens.spaceEight))
                    Text(text = locString(R.string.crop_dialog_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(dimens.spaceLarge))

                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }
                    var viewSize by remember { mutableStateOf(200f) }

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .onGloballyPositioned { viewSize = it.size.width.toFloat() }
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.05f))
                            .border(dimens.borderThick, MaterialTheme.colorScheme.primary, CircleShape)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(dimens.spaceLarge))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)) {
                        OutlinedButton(onClick = { showCropDialog = false }, modifier = Modifier.weight(1f)) {
                            Text(locString(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val croppedUri = cropBitmapAndSave(
                                    context = context,
                                    uri = selectedImageUri!!,
                                    scale = scale,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    viewSize = viewSize
                                )
                                if (croppedUri != null) {
                                    mainViewModel.updateAvatar(croppedUri.toString())
                                } else {
                                    mainViewModel.updateAvatar(selectedImageUri.toString())
                                }
                                showCropDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(locString(R.string.confirm))
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = dimens.spaceLarge, end = dimens.spaceLarge, bottom = dimens.spaceBottomOverScroll),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraLarge)
    ) {
        item { Spacer(modifier = Modifier.height(dimens.spaceTwelve)) }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val avatarScale = if (isPremium) 1.04f else 1f
                val pulse = rememberInfiniteTransition(label = "")
                val pulseAlpha by pulse.animateFloat(
                    initialValue = 0.30f,
                    targetValue = 0.70f,
                    animationSpec = infiniteRepeatable(animation = tween(1300, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                    label = ""
                )

                Box(modifier = Modifier.size(dimens.sizeAvatarFrame).scale(avatarScale), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(dimens.sizeAvatarBig)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(if (isPremium) Color(0xFFFFF3C4) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(width = if (isPremium) dimens.borderHeavy else dimens.zero, color = if (isPremium) colors.premiumGoldAccent else Color.Transparent, shape = CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPremium) {
                            Box(modifier = Modifier.matchParentSize().clip(CircleShape).background(colors.premiumGoldAccent.copy(alpha = pulseAlpha)))
                        }

                        if (avatarUrl.isNotEmpty()) {
                            SubcomposeAsyncImage(
                                model = avatarUrl,
                                contentDescription = displayName,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant)) }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = displayName,
                                modifier = Modifier.size(dimens.sizeAvatarMedium),
                                tint = if (isPremium) Color(0xFF8D6E00) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isPremium) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = -dimens.spaceTwo, y = -dimens.spaceTwo).size(dimens.sizeScrollbarTrack).clip(CircleShape).background(colors.premiumGoldAccent).border(dimens.borderMedium, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = colors.premiumGoldTextDark, modifier = Modifier.size(dimens.sizeIconSmall))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spaceNormal))
                Text(text = displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(dimens.spaceFour))
                Text(text = email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

        item {
            val shimmerTransition = rememberInfiniteTransition(label = "")
            val shimmerOffset by shimmerTransition.animateFloat(
                initialValue = -500f, targetValue = 1500f,
                animationSpec = infiniteRepeatable(animation = tween(durationMillis = 2500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                label = ""
            )

            val premiumGradient = if (isPremium) {
                Brush.linearGradient(
                    colors = listOf(colors.premiumGoldDark, colors.premiumGoldAccent, colors.premiumGoldLight, colors.premiumGoldAccent, colors.premiumGoldDark),
                    start = androidx.compose.ui.geometry.Offset(x = shimmerOffset, y = 0f),
                    end = androidx.compose.ui.geometry.Offset(x = shimmerOffset + 400f, y = 400f)
                )
            } else {
                Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.dialog,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.background(premiumGradient).padding(dimens.spaceLarge).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (isPremium) locString(R.string.premium_user_badge) else locString(R.string.regular_user_badge), style = MaterialTheme.typography.titleMedium, color = if (isPremium) colors.premiumGoldTextDark else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(dimens.spaceTwelve))
                        Button(
                            onClick = { mainViewModel.setPremium(!isPremium) }, 
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPremium) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary), 
                            shape = shapes.button
                        ) {
                            Text(if (isPremium) locString(R.string.cancel_premium) else locString(R.string.upgrade_to_premium), style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                    Icon(imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.StarOutline, contentDescription = null, tint = if (isPremium) colors.premiumGoldTextDark else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(dimens.sizeIconLarge))
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = shapes.dialog, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(dimens.spaceNormal)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Contrast, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(dimens.spaceNormal))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = locString(R.string.theme_settings), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(dimens.spaceTwelve))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceEight)) {
                                listOf("system" to R.string.theme_system, "dark" to R.string.theme_dark, "light" to R.string.theme_light).forEach { (mode, nameRes) ->
                                    MinimalChip(text = locString(nameRes), isSelected = currentTheme == mode, onClick = { mainViewModel.updateTheme(mode) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = dimens.spaceNormal), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(dimens.spaceNormal))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = locString(R.string.language_settings), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(dimens.spaceTwelve))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceEight)) {
                                listOf("en" to R.string.english_lang, "fa" to R.string.persian_lang).forEach { (lang, nameRes) ->
                                    MinimalChip(text = locString(nameRes), isSelected = currentLang == lang, onClick = { mainViewModel.updateLanguage(lang) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().clip(shapes.button).clickable { mainViewModel.logout() }.padding(vertical = dimens.spaceTwelve), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(dimens.sizeIconNormal))
                    Spacer(modifier = Modifier.width(dimens.spaceEight))
                    Text(locString(R.string.logout), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(dimens.spaceHuge))
        }
    }
}

fun cropBitmapAndSave(context: Context, uri: Uri, scale: Float, offsetX: Float, offsetY: Float, viewSize: Float): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream?.close()

        val bitmapWidth = originalBitmap.width
        val bitmapHeight = originalBitmap.height

        val viewAspectRatio = viewSize / viewSize
        val bitmapAspectRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()

        val baseScale = if (bitmapAspectRatio > viewAspectRatio) {
            viewSize / bitmapHeight.toFloat()
        } else {
            viewSize / bitmapWidth.toFloat()
        }

        val finalScale = baseScale * scale

        val matrix = Matrix()
        matrix.postScale(finalScale, finalScale)

        val displayWidth = bitmapWidth * finalScale
        val displayHeight = bitmapHeight * finalScale

        val left = ((displayWidth - viewSize) / 2f - offsetX) / finalScale
        val top = ((displayHeight - viewSize) / 2f - offsetY) / finalScale
        val width = viewSize / finalScale
        val height = viewSize / finalScale

        val cropX = left.toInt().coerceIn(0, (bitmapWidth - 1))
        val cropY = top.toInt().coerceIn(0, (bitmapHeight - 1))
        val cropW = width.toInt().coerceIn(1, (bitmapWidth - cropX))
        val cropH = height.toInt().coerceIn(1, (bitmapHeight - cropY))

        val croppedBitmap = Bitmap.createBitmap(originalBitmap, cropX, cropY, cropW, cropH)

        val croppedFile = File(context.cacheDir, "cropped_avatar_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(croppedFile)
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()

        Uri.fromFile(croppedFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun MinimalChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp))
    }
}