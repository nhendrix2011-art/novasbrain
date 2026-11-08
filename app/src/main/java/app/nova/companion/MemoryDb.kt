package app.nova.companion

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class ChatMessage(@PrimaryKey(autoGenerate = true) val id: Long = 0, val role: String, val content: String, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "memories", indices = [Index(value = ["text"], unique = true)])
data class Memory(@PrimaryKey(autoGenerate = true) val id: Long = 0, val text: String, val category: String = "general", val confidence: String = "tentative", val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis())

@Dao
interface NovaDao {
    @Query("SELECT * FROM messages ORDER BY createdAt DESC LIMIT 60") fun recentMessages(): Flow<List<ChatMessage>>
    @Insert suspend fun addMessage(message: ChatMessage)
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC LIMIT 100") fun allMemories(): Flow<List<Memory>>
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC LIMIT 80") suspend fun memoryContext(): List<Memory>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addMemory(memory: Memory)
    @Query("DELETE FROM memories WHERE id = :id") suspend fun deleteMemory(id: Long)
    @Query("DELETE FROM messages") suspend fun clearChat()
}
@Database(entities = [ChatMessage::class, Memory::class], version = 1, exportSchema = false)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun dao(): NovaDao
    companion object {
        @Volatile private var INSTANCE: NovaDatabase? = null
        fun get(context: Context): NovaDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context, NovaDatabase::class.java, "nova-local.db").build().also { INSTANCE = it }
        }
    }
}
