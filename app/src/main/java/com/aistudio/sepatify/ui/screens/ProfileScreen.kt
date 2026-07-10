package com.aistudio.sepatify.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    mainViewModel: MainViewModel,
    locString: (Int) -> String
) {
    val displayName by mainViewModel.userDisplayName.collectAsState()
    val email by mainViewModel.userEmail.collectAsState()
    val avatarUrl by mainViewModel.userAvatar.collectAsState()
    val isPremium by mainViewModel.isPremium.collectAsState()
    val currentTheme by mainViewModel.currentTheme.collectAsState()
    val currentLang by mainViewModel.currentLanguage.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            mainViewModel.updateAvatar(it.toString())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // --- بخش آواتار با افکت درخشش طلایی پالس‌دار برای اعضای پرمیوم ---
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // تعریف انیمیشن پالس نوری برای حالت پرمیوم
                val avatarScale = if (isPremium) 1.04f else 1f
                val pulse = rememberInfiniteTransition(label = "profileAvatarPulse")
                val pulseAlpha by pulse.animateFloat(
                    initialValue = 0.30f,
                    targetValue = 0.70f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1300, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "profileAvatarPulseAlpha"
                )

                // باکس اصلی ریشه (بدون کلیپ برای جلوگیری از کات شدن لبه ستاره)
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(avatarScale),
                    contentAlignment = Alignment.Center
                ) {

                    // دایره اصلی و بستر آواتار
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(if (isPremium) Color(0xFFFFF3C4) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isPremium) 3.dp else 0.dp,
                                color = if (isPremium) Color(0xFFFFD54F) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // هاله درخشنده متحرک (فقط برای کاربر پرمیوم)
                        if (isPremium) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD54F).copy(alpha = pulseAlpha))
                            )
                        }

                        if (avatarUrl.isNotEmpty()) {
                            SubcomposeAsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                loading = {
                                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant))
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = if (isPremium) Color(0xFF8D6E00) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // بج ستاره طلایی درخشان روی لبه پایینی آواتار پرمیوم
                    if (isPremium) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD54F))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Premium",
                                tint = Color(0xFF7A4F00),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

// --- کارت پرمیوم با گرادینت طلایی درخشان و افکت نوری متحرک ---
        item {
            // ایجاد انیمیشن بی‌انتها برای حرکت دادن افکت درخشش طلایی
            val shimmerTransition = rememberInfiniteTransition(label = "premiumCardShimmer")
            val shimmerOffset by shimmerTransition.animateFloat(
                initialValue = -500f,
                targetValue = 1500f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmerOffset"
            )

            // تعریف رنگ‌های طیف طلایی (لوکس و براق)
            val goldBase = Color(0xFFF1C40F)       // طلایی اصلی
            val goldLight = Color(0xFFFDE16D)      // طلایی روشن (نور درخشش)
            val goldDark = Color(0xFFD4AC0D)       // طلایی تیره برای سایه روشن

            val premiumGradient = if (isPremium) {
                // ایجاد یک گرادینت متحرک مایل (Diagonal) که افکت درخشش را شبیه‌سازی می‌کند
                Brush.linearGradient(
                    colors = listOf(goldDark, goldBase, goldLight, goldBase, goldDark),
                    start = Offset(x = shimmerOffset, y = 0f),
                    end = Offset(x = shimmerOffset + 400f, y = 400f)
                )
            } else {
                Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier
                        .background(premiumGradient)
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPremium) locString(R.string.premium_user_badge) else locString(R.string.regular_user_badge),
                            style = MaterialTheme.typography.titleMedium,
                            // تغییر رنگ متن به قهوه‌ای تیره لوکس در حالت پرمیوم برای کنتراست عالی روی پس‌زمینه طلایی
                            color = if (isPremium) Color(0xFF4A3700) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isPremium) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { mainViewModel.setPremium(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(locString(R.string.upgrade_to_premium), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Icon(
                        imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.StarOutline,
                        contentDescription = null,
                        tint = if (isPremium) Color(0xFF4A3700) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // --- منوی تنظیمات ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // تنظیمات پوسته (Theme)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Contrast,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = locString(R.string.theme_settings),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("system" to R.string.theme_system, "dark" to R.string.theme_dark, "light" to R.string.theme_light).forEach { (mode, nameRes) ->
                                    val isSelected = currentTheme == mode
                                    MinimalChip(
                                        text = locString(nameRes),
                                        isSelected = isSelected,
                                        onClick = { mainViewModel.updateTheme(mode) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // تنظیمات زبان (Language)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = locString(R.string.language_settings),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("en" to R.string.english_lang, "fa" to R.string.persian_lang).forEach { (lang, nameRes) ->
                                    val isSelected = currentLang == lang
                                    MinimalChip(
                                        text = locString(nameRes),
                                        isSelected = isSelected,
                                        onClick = { mainViewModel.updateLanguage(lang) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- دکمه خروج مینیمال ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { mainViewModel.logout() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(locString(R.string.logout), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- کامپوننت چیپ اصلاح شده بدون باگ حذف حروف ---
@Composable
fun MinimalChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp)
        )
    }
}