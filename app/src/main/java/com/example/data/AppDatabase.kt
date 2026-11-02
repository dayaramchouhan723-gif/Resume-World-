package com.example.data

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val skills: String = "",
    val experience: String = "",
    val education: String = "",
    val baseResumePath: String = ""
)

@Entity(tableName = "applied_jobs")
data class AppliedJobsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val company: String,
    val role: String,
    val dateApplied: Long = System.currentTimeMillis(),
    val status: String = "Applied",
    val resumePath: String = "",
    val jobLink: String = "",
    val chatHistory: String = "Sanam: Boss, aapka application Submit ho gaya hai! Jab reply aaye to yahan batayein."
)

@Entity(tableName = "vault")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentName: String,
    val filePath: String = "",
    val extractedText: String = "",
    val uploadDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "cover_letters")
data class CoverLetterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val companyName: String,
    val targetCountry: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorName: String,
    val country: String,
    val companyName: String,
    val message: String,
    val datePosted: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isVerified: Boolean = true
)

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
}

@Dao
interface AppDao {
    @Query("SELECT * FROM applied_jobs ORDER BY dateApplied DESC")
    fun getAllAppliedJobs(): Flow<List<AppliedJobsEntity>>

    @Query("SELECT * FROM applied_jobs ORDER BY dateApplied DESC")
    fun getAllAppliedJobsSync(): List<AppliedJobsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppliedJob(job: AppliedJobsEntity)

    @Query("DELETE FROM applied_jobs WHERE id = :id")
    suspend fun deleteAppliedJobById(id: Int)

    @Query("SELECT * FROM vault ORDER BY uploadDate DESC")
    fun getVaultDocs(): Flow<List<VaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultDoc(doc: VaultEntity)

    @Query("DELETE FROM vault WHERE id = :id")
    suspend fun deleteVaultDocById(id: Int)

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM cover_letters ORDER BY createdAt DESC")
    fun getAllCoverLetters(): Flow<List<CoverLetterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoverLetter(letter: CoverLetterEntity)

    @Query("SELECT * FROM community_posts ORDER BY datePosted DESC")
    fun getAllCommunityPosts(): Flow<List<CommunityPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunityPost(post: CommunityPostEntity)
}

@Database(
    entities = [
        ResumeDraft::class,
        UserProfileEntity::class,
        AppliedJobsEntity::class,
        VaultEntity::class,
        CoverLetterEntity::class,
        CommunityPostEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sanam_ai_resume.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
