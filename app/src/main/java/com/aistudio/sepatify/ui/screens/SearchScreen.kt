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
import com.aistudio.sepatify.ui.theme.sepatifyColors
import com.aistudio.sepatify.ui.theme.sepatifyDimens
import com.aistudio.sepatify.ui.theme.sepatifyShapes
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
    val colors = MaterialTheme.sepatifyColors
    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(dimens.spaceNormal)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { searchViewModel.updateSearchQuery(it) },
            placeholder = { Text(locString(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = locString(R.string.nav_search)) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { searchViewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = locString(R.string.clear_history))
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.chip
        )

        Spacer(modifier = Modifier.height(dimens.spaceTwelve))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceEight)
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

        Spacer(modifier = Modifier.height(dimens.spaceNormal))

        when (val state = uiState) {
            SearchUiState.Idle -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceNormal),
                    contentPadding = PaddingValues(bottom = dimens.spaceBottomOverScroll),
                    modifier = Modifier.weight(1f).fillMaxWidth()
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
                                    .padding(vertical = dimens.spaceFour),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceTen)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = locString(R.string.search_history),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                    Text(text = historyItem, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { searchViewModel.deleteHistoryItem(historyItem) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = locString(R.string.clear_history),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = locString(R.string.search_top_genres),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = dimens.spaceFour)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = locString(R.string.genre_pop),
                                    backgroundColor = colors.genrePop,
                                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Pop") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = locString(R.string.genre_indie),
                                    backgroundColor = colors.genreIndie,
                                    imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Indie") }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = locString(R.string.genre_rock),
                                    backgroundColor = colors.genreRock,
                                    imageUrl = "https://images.unsplash.com/photo-1487180142328-0c4e37023af5?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Rock") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = locString(R.string.genre_r_b),
                                    backgroundColor = colors.genreRandB,
                                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("R&B") }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = locString(R.string.search_browse_all),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = dimens.spaceFour)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = locString(R.string.genre_podcasts),
                                    backgroundColor = colors.genrePodcasts,
                                    imageUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Podcasts") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = locString(R.string.genre_made_for_you),
                                    backgroundColor = colors.genreMadeForYou,
                                    imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Made For You") }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = locString(R.string.genre_charts),
                                    backgroundColor = colors.genreCharts,
                                    imageUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("Charts") }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrowseGenreCard(
                                    title = locString(R.string.genre_new_releases_tag),
                                    backgroundColor = colors.genreNewReleases,
                                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150&q=80",
                                    onClick = { searchViewModel.updateSearchQuery("New Releases") }
                                )
                            }
                        }
                    }
                }
            }
            SearchUiState.Loading -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceFour)
                ) {
                    items(8) { SongRowSkeleton() }
                }
            }
            is SearchUiState.Success -> {
                val pagedItems = searchViewModel.pagedResults.collectAsLazyPagingItems()
                if (pagedItems.itemCount == 0) {
                    EmptyStateView(
                        icon = Icons.Default.SearchOff,
                        title = locString(R.string.no_results),
                        subtitle = locString(R.string.empty_search_subtitle),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceTen),
                        contentPadding = PaddingValues(bottom = dimens.spaceBottomOverScroll),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(pagedItems.itemCount, key = pagedItems.itemKey { it.id }) { index ->
                            val song = pagedItems[index] ?: return@items
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSongSelect(song, state.results) }
                                    .padding(vertical = dimens.spaceSix),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                            ) {
                                val art = rememberSongArt(song)
                                AsyncImage(
                                    model = art,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(dimens.sizeSongThumbnailNormal)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text(text = song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = song.title, tint = MaterialTheme.colorScheme.primary)
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
    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.heightGenreCard)
            .clickable { onClick() },
        shape = shapes.small,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shapes.small)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimens.spaceTwelve)
            )

            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = dimens.spaceNormal, y = dimens.spaceNormal)
                    .size(dimens.spaceGiant)
                    .graphicsLayer { rotationZ = 25f }
                    .clip(MaterialTheme.shapes.extraSmall)
            )
        }
    }
}