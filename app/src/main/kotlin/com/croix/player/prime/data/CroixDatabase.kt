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
    val playedAtMs: Long = System.currentTimeMillis(),
) {
    fun toTrack() = Track(id, title, artist, album, durationSeconds, thumbnailUrl)
}

@Dao
interface CroixDao {
    @Query("SELECT * FROM favorites ORDER BY addedAtMs DESC")
    fun observeFavorites(): Flow<List<FavoriteTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteTrackEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT * FROM history ORDER BY playedAtMs DESC LIMIT 60")
    fun observeHistory(): Flow<List<HistoryTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPlay(item: HistoryTrackEntity)
}

@Database(
    entities = [FavoriteTrackEntity::class, HistoryTrackEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CroixDatabase : RoomDatabase() {
    abstract fun dao(): CroixDao

    companion object {
        @Volatile
        private var instance: CroixDatabase? = null

        fun get(context: Context): CroixDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CroixDatabase::class.java,
                    "croix_prime.db",
                ).build().also { instance = it }
            }
    }
}
