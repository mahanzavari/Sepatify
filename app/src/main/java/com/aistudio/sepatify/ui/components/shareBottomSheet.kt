package com.aistudio.sepatify.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.local.PlaylistEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.theme.sepatifyColors
import com.aistudio.sepatify.ui.theme.sepatifyDimens
import com.aistudio.sepatify.ui.theme.sepatifyShapes
import com.aistudio.sepatify.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    song: Song? = null,
    playlist: PlaylistEntity? = null,
    chatViewModel: ChatViewModel,
    onDismiss: () -> Unit,
    locString: (Int) -> String
) {
    val context = LocalContext.current
    val followedUsers by chatViewModel.followedUsers.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val titleToShare = song?.title ?: playlist?.title ?: ""

    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes
    val colors = MaterialTheme.sepatifyColors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceEight)
        ) {
            Text(
                text = stringResource(R.string.share_with, titleToShare),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(dimens.spaceLarge))

            // Option 1: Share Externally via Android Intent
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        shareExternally(context, song, playlist, locString)
                        coroutineScope.launch { sheetState.hide(); onDismiss() }
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = shapes.button
            ) {
                Row(
                    modifier = Modifier
                        .padding(dimens.spaceNormal)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceNormal)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = locString(R.string.share_via_apps),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spaceLarge))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = dimens.alphaGrooves * 2.5f))
            Spacer(modifier = Modifier.height(dimens.spaceNormal))

            // Option 2: Send internally inside Sepatify Chat System
            Text(
                text = locString(R.string.send_to_friends),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = dimens.alphaStandard)
            )

            Spacer(modifier = Modifier.height(dimens.spaceTwelve))

            if (followedUsers.isEmpty()) {
                Text(
                    text = locString(R.string.no_friends),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = dimens.alphaMuted),
                    modifier = Modifier.padding(vertical = dimens.spaceNormal)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = dimens.heightMaxLazyColumn),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceEight),
                    contentPadding = PaddingValues(bottom = dimens.spaceLarge)
                ) {
                    items(followedUsers) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.small)
                                .clickable {
                                    sendInternalMessage(user, song, playlist, chatViewModel, context, locString)
                                    coroutineScope.launch { sheetState.hide(); onDismiss() }
                                }
                                .padding(dimens.spaceTwelve),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceNormal)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dimens.sizeAvatarNormal)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            alpha = dimens.alphaGrooves * 3.75f
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = user,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(dimens.aspectRatioSquare)
                            )
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shareExternally(context: Context, song: Song?, playlist: PlaylistEntity?, locString: (Int) -> String) {
    val shareText = if (song != null) {
        String.format(locString(R.string.share_song_text), song.title, song.artistName)
    } else if (playlist != null) {
        String.format(locString(R.string.share_playlist_text), playlist.title)
    } else ""

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    val chooserIntent = Intent.createChooser(intent, locString(R.string.share))
    // FIX: A ConfigurationContext lacks standard Activity start behavior.
    // Adding FLAG_ACTIVITY_NEW_TASK allows the Android Framework to safely launch the Share Sheet natively.
    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    context.startActivity(chooserIntent)
}

private fun sendInternalMessage(user: String, song: Song?, playlist: PlaylistEntity?, chatViewModel: ChatViewModel, context: Context, locString: (Int) -> String) {
    if (song != null) {
        chatViewModel.sendMessage(user, "Check out this song!", song)
    } else if (playlist != null) {
        chatViewModel.sendMessage(user, "Check out this playlist: ${playlist.title}\n${playlist.description}", null)
    }
    Toast.makeText(context, String.format(locString(R.string.sent_success), user), Toast.LENGTH_SHORT).show()
}