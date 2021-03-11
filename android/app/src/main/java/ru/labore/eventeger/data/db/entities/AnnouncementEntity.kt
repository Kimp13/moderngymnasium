package ru.labore.eventeger.data.db.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.threeten.bp.ZonedDateTime
import ru.labore.eventeger.data.db.ZonedDateTimeParceler

@Parcelize
@TypeParceler<ZonedDateTime, ZonedDateTimeParceler>
@Entity(tableName = "announcement")
data class AnnouncementEntity(
    @PrimaryKey
    override val id: Int,
    override val authorId: Int,
    override var commentsCount: Int,
    override val text: String,
    override var createdAt: ZonedDateTime,
    override var updatedAt: ZonedDateTime,
    val isEvent: Boolean,
    var startsAt: ZonedDateTime? = null,
    var endsAt: ZonedDateTime? = null
) : AuthoredEntity, Parcelable