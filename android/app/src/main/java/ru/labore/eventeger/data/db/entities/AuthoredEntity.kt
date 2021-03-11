package ru.labore.eventeger.data.db.entities

import android.os.Parcelable
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.threeten.bp.ZonedDateTime
import ru.labore.eventeger.data.db.ZonedDateTimeParceler

interface AuthoredEntity {
    val id: Int
    val authorId: Int
    var commentsCount: Int
    val text: String
    var createdAt: ZonedDateTime
    var updatedAt: ZonedDateTime
}