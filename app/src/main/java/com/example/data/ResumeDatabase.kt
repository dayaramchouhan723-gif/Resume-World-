package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resume_drafts ORDER BY updatedAt DESC")
    fun getAllDrafts(): Flow<List<ResumeDraft>>

    @Query("SELECT * FROM resume_drafts ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDraft(): ResumeDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: ResumeDraft): Long

    @Query("DELETE FROM resume_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Long)

    @Query("DELETE FROM resume_drafts")
    suspend fun deleteAllDrafts()
}

@Database(entities = [ResumeDraft::class], version = 1, exportSchema = false)
abstract class ResumeDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao

    companion object {
        @Volatile
        private var INSTANCE: ResumeDatabase? = null

        fun getInstance(context: Context): ResumeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ResumeDatabase::class.java,
                    "canadian_resume_ats.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ResumeRepository(private val dao: ResumeDao) {
    val allDrafts: Flow<List<ResumeDraft>> = dao.getAllDrafts()

    suspend fun getLatestDraft(): ResumeDraft? = dao.getLatestDraft()

    suspend fun saveDraft(draft: ResumeDraft): Long = dao.upsertDraft(draft)

    suspend fun deleteDraft(id: Long) = dao.deleteDraftById(id)
}
