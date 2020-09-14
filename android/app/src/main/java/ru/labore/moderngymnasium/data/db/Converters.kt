package ru.labore.moderngymnasium.data.db

import androidx.room.TypeConverter
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class DateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    @TypeConverter
    fun fromZonedDateTime(date: ZonedDateTime?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun toZonedDateTime(value: String?): ZonedDateTime? {
        return value?.let {
            return formatter.parse(value, ZonedDateTime::from)
        }
    }
}