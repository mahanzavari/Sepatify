package com.aistudio.sepatify.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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

    var showPremiumSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = locString(R.string.profile_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Avatar & Info Section
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .clickable {
                            // Simulate updating avatar
                            mainViewModel.updateAvatar("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&q=80")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(text = email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }

        // Premium Status Card (Golden UI)
        item {
            AnimatedContent(targetState = isPremium, label = "PremiumCard") { premium ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (premium) Color(0xFFC5A030) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (premium) locString(R.string.premium_user_badge) else locString(R.string.regular_user_badge),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (premium) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            if (!premium) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { 
                                        mainViewModel.setPremium(true) 
                                        showPremiumSuccess = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(locString(R.string.upgrade_to_premium))
                                }
                            }
                        }
                        Icon(
                            imageVector = if (premium) Icons.Default.WorkspacePremium else Icons.Default.StarOutline,
                            contentDescription = "Premium Badge",
                            tint = if (premium) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        // Settings Toggles (Theme & Language)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = locString(R.string.drawer_settings_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Theme Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(locString(R.string.theme_settings), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("system" to R.string.theme_system, "dark" to R.string.theme_dark, "light" to R.string.theme_light).forEach { (mode, nameRes) ->
                                FilterChip(
                                    selected = currentTheme == mode,
                                    onClick = { mainViewModel.updateTheme(mode) },
                                    label = { Text(locString(nameRes)) }
                                )
                            }
                        }
                    }
                }

                // Language Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(locString(R.string.language_settings), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("en" to R.string.english_lang, "fa" to R.string.persian_lang).forEach { (lang, nameRes) ->
                                FilterChip(
                                    selected = currentLang == lang,
                                    onClick = { mainViewModel.updateLanguage(lang) },
                                    label = { Text(locString(nameRes)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { mainViewModel.logout() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(locString(R.string.logout), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(60.dp)) // Avoid overlap with bottom nav
        }
    }

    if (showPremiumSuccess) {
        AlertDialog(
            onDismissRequest = { showPremiumSuccess = false },
            title = { Text("Premium Activated") },
            text = { Text(locString(R.string.premium_unlocked_dialog)) },
            confirmButton = {
                Button(onClick = { showPremiumSuccess = false }) {
                    Text(locString(R.string.ok_label))
                }
            }
        )
    }
}