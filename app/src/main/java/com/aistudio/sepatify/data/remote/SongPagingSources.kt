package com.aistudio.sepatify.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.remote.dto.PlaylistSongJoinDto
import com.aistudio.sepatify.data.remote.dto.SongDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

private const val PAGE_SIZE = 20

private fun SongDto.toDomain() = Song(id, title, artistName, coverImageUrl, audioUrl, category, artistId)

/** Paginates the `songs` catalog through Postgrest `.range()`, used for text search (FR: Paging 3 for search results). */
class SongSearchPagingSource(private val query: String) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val page = params.key ?: 0
        val from = page * PAGE_SIZE
        val to = from + PAGE_SIZE - 1
        return try {
            val results = Supa.client.from("songs")
                .select(columns = Columns.ALL) {
                    if (query.isNotBlank()) {
                        filter {
                            or {
                                ilike("title", "%$query%")
                                ilike("artist_name", "%$query%")
                            }
                        }
                    }
                    order("title", Order.ASCENDING)
                    range(from.toLong(), to.toLong())
                }
                .decodeList<SongDto>()
                .map { it.toDomain() }

            LoadResult.Page(
                data = results,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (results.size < PAGE_SIZE) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

/** Paginates the songs inside a Supabase-backed playlist (Global / User playlists). */
class PlaylistSongsPagingSource(private val playlistId: Long) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val page = params.key ?: 0
        val from = page * PAGE_SIZE
        val to = from + PAGE_SIZE - 1
        return try {
            val results = Supa.client.from("playlist_songs")
                .select(columns = Columns.raw("playlist_id, song_id, songs(*)")) {
                    filter { eq("playlist_id", playlistId) }
                    order("position", Order.ASCENDING)
                    range(from.toLong(), to.toLong())
                }
                .decodeList<PlaylistSongJoinDto>()
                .map { it.songs.toDomain() }

            LoadResult.Page(
                data = results,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (results.size < PAGE_SIZE) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
