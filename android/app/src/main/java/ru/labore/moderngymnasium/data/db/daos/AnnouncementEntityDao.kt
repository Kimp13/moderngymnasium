package ru.labore.moderngymnasium.data.db.daos

import androidx.room.*
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity

@Dao
interface AnnouncementEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(announcement: AnnouncementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArray(announcements: Array<AnnouncementEntity>)

    @Query("""
        select count(id) from announcement
    """)
    suspend fun countAnnouncements(): Int

    @Query("""
        select * from announcement where id = :id
    """)
    suspend fun getAnnouncement(id: Int): AnnouncementEntity?

    @Query("""
        select * from announcement
        where createdAt < :offset
        order by createdAt desc
        limit 1
    """)
    suspend fun getAnnouncementAtOffset(offset: ZonedDateTime): AnnouncementEntity?

    @Transaction
    @Query("""
        select * from announcement
        where createdAt < :offset
        order by createdAt desc
        limit :limit
    """)
    suspend fun getAnnouncements(offset: ZonedDateTime, limit: Int): Array<AnnouncementEntity>
}
