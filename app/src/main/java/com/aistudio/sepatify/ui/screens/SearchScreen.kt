package com.aistudio.sepatify.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.viewmodel.SearchUiState
import com.aistudio.sepatify.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    onSongSelect: (Song, List<Song>) -> Unit,
    locString: (Int) -> String
) {
    val query by searchViewModel.searchQuery.collectAsState()
    val filter by searchViewModel.selectedFilter.collectAsState()
    val uiState by searchViewModel.uiState.collectAsState()
    val history by searchViewModel.searchHistory.collectAsState()

    val filters = listOf("All", "Songs", "Artists")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = { searchViewModel.updateSearchQuery(it) },
            placeholder = { Text(locString(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { searchViewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips (Songs, Artists)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filterItem ->
                val localizedFilter = when (filterItem) {
                    "All" -> locString(R.string.filter_all)
                    "Songs" -> locString(R.string.filter_songs)
                    else -> locString(R.string.filter_artists)
                }

                FilterChip(
                    selected = (filter == filterItem),
                    onClick = { searchViewModel.setFilter(filterItem) },
                    label = { Text(localizedFilter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Content
        when (val state = uiState) {
            SearchUiState.Idle -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (history.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = locString(R.string.search_history),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { searchViewModel.clearHistory() }) {
                                    Text(locString(R.string.clear_history), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        items(history) { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { searchViewModel.updateSearchQuery(historyItem) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = "History item icon",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                    Text(text = historyItem, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { searchViewModel.deleteHistoryItem(historyItem) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete from history",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Your top genres",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = "Pop",
                                    backgroundColor = Color(0xFF27856A),
                                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Pop") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = "Indie",
                                    backgroundColor = Color(0xFF477C2B),
                                    imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Indie") }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = "Rock",
                                    backgroundColor = Color(0xFFE8115B),
                                    imageUrl = "https://images.unsplash.com/photo-1487180142328-0c4e37023af5?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Rock") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = "R&B",
                                    backgroundColor = Color(0xFFD84080),
                                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("R&B") }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Browse all",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = "Podcasts",
                                    backgroundColor = Color(0xFF2296F3),
                                    imageUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Podcasts") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = "Made For You",
                                    backgroundColor = Color(0xFF1E3264),
                                    imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Made For You") }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = "Charts",
                                    backgroundColor = Color(0xFF8D67AB),
                                    imageUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Charts") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = "New Releases",
                                    backgroundColor = Color(0xFFE1306C),
                                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("New Releases") }
                                )
                            }
                        }
                    }
                }
            }
            SearchUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is SearchUiState.Success -> {
                // Paginated via Paging 3 (Postgrest `.range()` under the hood) instead of loading
                // the whole result set into a plain LazyColumn.
                val pagedItems = searchViewModel.pagedResults.collectAsLazyPagingItems()
                if (pagedItems.itemCount == 0) {
                    EmptyStateView(
                        icon = Icons.Default.SearchOff,
                        title = locString(R.string.no_results),
                        subtitle = locString(R.string.empty_search_subtitle),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pagedItems.itemCount, key = pagedItems.itemKey { it.id }) { index ->
                            val song = pagedItems[index] ?: return@items
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSongSelect(song, state.results) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val art = rememberSongArt(song)
                                AsyncImage(
                                    model = art,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text(text = song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play icon", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseGenreCard(
    title: String,
    backgroundColor: Color,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 16.dp, y = 16.dp)
                    .size(64.dp)
                    .graphicsLayer { rotationZ = 25f }
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}
