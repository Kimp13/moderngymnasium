package ru.labore.moderngymnasium.utils

import com.google.gson.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.data.sharedpreferences.entities.AllPermissions
import java.lang.reflect.Type

class JsonPermissionsDeserializerImpl : JsonDeserializer<AllPermissions> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): AllPermissions? {
        return if (json != null && json.isJsonObject) {
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
    ): JsonElement {
        return if (src == null) {
            JsonPrimitive(false)
        } else {
            if (src.all) {
                JsonPrimitive(true)
            } else {
                val result = JsonObject()

                if (src.announcement != null) {
                    result.add("announcement", src.announcement.serialize())
                }

                if (src.profile != null) {
                    result.add("profile", src.profile.serialize())
                }

                result
            }
        }
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