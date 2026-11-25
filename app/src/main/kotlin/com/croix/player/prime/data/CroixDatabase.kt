/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.croix.prime.engine.Track
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites")
data class FavoriteTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val addedAtMs: Long = System.currentTimeMillis(),
) {
    fun toTrack() = Track(id, title, artist, album, durationSeconds, thumbnailUrl)
}

@Entity(tableName = "history")
data class HistoryTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val playCount: Int = 1,
    val playedAtMs: Long = System.currentTimeMillis(),
) {
    fun toTrack() = Track(id, title, artist, album, durationSeconds, thumbnailUrl)
}

@Entity(tableName = "play_events")
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val eventId: Long = 0L,
    val trackId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val playedAtMs: Long = System.currentTimeMillis(),
    val hourOfDay: Int, // 0..23
    val dayOfWeek: Int, // 0=Mon .. 6=Sun
)

@Entity(tableName = "blocked_artists")
data class BlockedArtistEntity(
    @PrimaryKey val artistKey: String,
    val displayName: String,
    val blockedAtMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "downloads")
data class DownloadedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val fileSizeBytes: Long,
    val downloadedAtMs: Long = System.currentTimeMillis(),
) {
    fun toTrack() = Track(id, title, artist, album, durationSeconds, thumbnailUrl)
}

@Entity(tableName = "playlists")
data class UserPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0L,
    val name: String,
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val addedAtMs: Long = System.currentTimeMillis(),
) {
    fun toTrack() = Track(trackId, title, artist, album, durationSeconds, thumbnailUrl)
}

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAtMs: Long = System.currentTimeMillis(),
)

data class ListeningStatsSummary(
    val totalPlays: Int = 0,
    val estimatedMinutes: Int = 0,
    val topArtists: List<Pair<String, Int>> = emptyList(),
    val topTracks: List<Pair<Track, Int>> = emptyList(),
    val hourlyPlays: List<Int> = List(24) { 0 },
    val weekdayPlays: List<Int> = List(7) { 0 },
    val peakHour: Int = -1,
    val peakWeekday: Int = -1,
)

@Dao
interface CroixDao {
    @Query("SELECT * FROM favorites ORDER BY addedAtMs DESC")
    fun observeFavorites(): Flow<List<FavoriteTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteTrackEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT * FROM history ORDER BY playedAtMs DESC LIMIT 200")
    fun observeHistory(): Flow<List<HistoryTrackEntity>>

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: String): HistoryTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: HistoryTrackEntity)

    @Query("SELECT * FROM play_events ORDER BY playedAtMs DESC LIMIT 1000")
    fun observePlayEvents(): Flow<List<PlayEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayEvent(event: PlayEventEntity)

    @Query("SELECT * FROM blocked_artists ORDER BY blockedAtMs DESC")
    fun observeBlockedArtists(): Flow<List<BlockedArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockArtist(artist: BlockedArtistEntity)

    @Query("DELETE FROM blocked_artists WHERE artistKey = :artistKey")
    suspend fun unblockArtist(artistKey: String)

    @Query("SELECT * FROM downloads ORDER BY downloadedAtMs DESC")
    fun observeDownloads(): Flow<List<DownloadedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(item: DownloadedTrackEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadRecord(id: String)

    @Query("SELECT * FROM playlists ORDER BY createdAtMs DESC")
    fun observePlaylists(): Flow<List<UserPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: UserPlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE playlistId = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT * FROM playlist_tracks ORDER BY addedAtMs DESC")
    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(entry: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Query("SELECT * FROM recent_searches ORDER BY searchedAtMs DESC LIMIT 12")
    fun observeRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecentSearch(item: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}

@Database(
    entities = [
        FavoriteTrackEntity::class,
        HistoryTrackEntity::class,
        PlayEventEntity::class,
        BlockedArtistEntity::class,
        DownloadedTrackEntity::class,
        UserPlaylistEntity::class,
        PlaylistTrackEntity::class,
        RecentSearchEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class CroixDatabase : RoomDatabase() {
    abstract fun dao(): CroixDao

    companion object {
        @Volatile
        private var instance: CroixDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `play_events` (
                        `eventId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `playedAtMs` INTEGER NOT NULL,
                        `hourOfDay` INTEGER NOT NULL,
                        `dayOfWeek` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blocked_artists` (
                        `artistKey` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `blockedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`artistKey`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): CroixDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CroixDatabase::class.java,
                    "croix_prime.db",
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
