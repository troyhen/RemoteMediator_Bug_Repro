package com.troy.remotemediatorbugrepro

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Database(entities = [Content::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract val contentDao: ContentDao
}

@Entity
data class Content(
    @PrimaryKey val cid: Int,
    val title: String?,
    val text: String?,
    val color: Int,
)

@Dao
interface ContentDao {

    @Query("select * from Content")
    fun getAll(): List<Content>

    @Insert
    fun insertAll(vararg content: Content)

    @Delete
    fun delete(content: Content)

    @Query("delete from Content")
    fun deleteAll()

    @Query("select * from Content")
    fun contentSource(): PagingSource<Int, Content>
}
