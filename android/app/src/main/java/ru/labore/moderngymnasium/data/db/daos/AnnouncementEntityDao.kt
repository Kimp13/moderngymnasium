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
        select not exists(select 1 from announcement
        limit 1 offset :offset) or (
        select
        announcement.updatedAt <
        datetime('now', 'localtime', '-1 day')
        from announcement
        limit 1 offset :offset)
    """)
    fun isAnnouncementUpdateNeeded(offset: Int): Boolean

    @Transaction
    @Query("""
        select * from announcement 
        join user on user.id = announcement.authorId
        limit :limit offset :offset
    """)
    suspend fun getAnnouncements(offset: Int, limit: Int): Array<AnnouncementWithAuthor>
}
