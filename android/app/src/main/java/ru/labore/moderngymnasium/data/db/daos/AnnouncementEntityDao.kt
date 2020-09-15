package ru.labore.moderngymnasium.data.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.db.entities.AnnouncementWithAuthor

@Dao
interface AnnouncementEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(announcements: Array<AnnouncementEntity>)

    @Query("""
        select * from announcement
        limit 1 offset :offset
    """)
    suspend fun getAnnouncementAtOffset(offset: Int): AnnouncementEntity?

    @Transaction
    @Query("""
        select * from announcement 
        join user on user.id = announcement.authorId
        limit :limit offset :offset
    """)
    suspend fun getAnnouncements(offset: Int, limit: Int): Array<AnnouncementWithAuthor>
}
