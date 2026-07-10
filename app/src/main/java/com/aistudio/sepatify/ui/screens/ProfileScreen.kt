package com.aistudio.sepatify.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // --- بخش آواتار و اطلاعات کاربری ---
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            mainViewModel.updateAvatar("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&q=80")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant))
                            }
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

        // --- کارت پرمیوم با گرادینت تیره و لوکس ---
        item {
            val premiumGradient = if (isPremium) {
                Brush.horizontalGradient(listOf(Color(0xFF232526), Color(0xFF414345)))
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
                            color = if (isPremium) Color(0xFFF1C40F) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        tint = if (isPremium) Color(0xFFF1C40F) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // --- منوی تنظیمات (با دکمه‌های هم‌سطح و فوق‌العاده فیت) ---
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
                        verticalAlignment = Alignment.CenterVertically // برای تراز شدن بهتر با دکمه‌های کوچک‌تر
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

// --- کامپوننت چیپ اصلاح شده با دکمه‌های بسیار فیت و شیک ---
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
            fontSize = 12.sp, // سایز فیکس، استاندارد و بسیار فیت برای دکمه‌های ۳ تایی
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false, // مانع از شکستن یا حذف حروف در حالت فشرده
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp) // پدینگ افقی کم برای باز شدن فضا جهت نمایش کامل کلمات
        )
    }
}