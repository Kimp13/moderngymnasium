package ru.labore.moderngymnasium.utils

import com.google.gson.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.data.sharedpreferences.entities.AllPermissions
import ru.labore.moderngymnasium.data.sharedpreferences.entities.AnnounceMap
import java.lang.reflect.Type

class JsonPermissionsDeserializerImpl : JsonDeserializer<AllPermissions> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): AllPermissions? {
        return if (json != null) {
            println(json.toString())

            if (json.isJsonObject) {
                AllPermissions(json.asJsonObject)
            } else {
                AllPermissions(json.asBoolean)
            }
        } else {
            null
        }
    }
}

class JsonPermissionsSerializerImpl : JsonSerializer<AllPermissions> {
    override fun serialize(
        src: AllPermissions?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement = when {
        src == null -> JsonPrimitive(false)
        src.all -> JsonPrimitive(true)
        else -> src.serialize()
    }
}

class JsonDateSerializerImpl : JsonSerializer<ZonedDateTime> {
    override fun serialize(
        src: ZonedDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src == null) {
            JsonPrimitive("")
        } else {
            JsonPrimitive(src.toString())
        }
    }
}

class JsonDateDeserializerImpl : JsonDeserializer<ZonedDateTime> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ZonedDateTime {
        return if (json == null) {
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC)
        } else {
            ZonedDateTime.parse(json.asString)
        }
    }
}

class JsonAnnounceMapDeserializerImpl : JsonDeserializer<AnnounceMap> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): AnnounceMap {
        return if (json != null && json.isJsonObject) {
            AnnounceMap(json.asJsonObject)
        } else {
            AnnounceMap()
        }
    }
}

class JsonAnnounceMapSerializerImpl : JsonSerializer<AnnounceMap> {
    override fun serialize(
        src: AnnounceMap?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement = when (src) {
        null -> JsonPrimitive(false)
        else -> src.serialize()
    }
}