package ru.labore.moderngymnasium.data.sharedpreferences.entities

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

class AnnounceMap {
    private val contents = HashMap<Int, ActionPermissionsTargets>()

    constructor ()

    constructor (json: JsonObject) {
        val entrySet: Set<Map.Entry<String?, JsonElement?>> = json.entrySet()

        for ((key, value) in entrySet) {
            if (key != null) {
                if (value?.isJsonArray == true) {
                    val array = value.asJsonArray

                    contents[key.toInt()] = ActionPermissionsTargets(
                        Array(array.size()) {
                            array[it].asInt
                        }
                    )
                } else {
                    contents[key.toInt()] = ActionPermissionsTargets(
                        value?.asBoolean ?: true
                    )
                }
            }
        }
    }

    operator fun get (key: Int): ActionPermissionsTargets =
        contents[key] ?: ActionPermissionsTargets(false)

    fun serialize () = when {
        contents.size == 0 -> JsonPrimitive(false)
        else -> {
            val result = JsonObject()

            for ((key, value) in contents) {
                val array = JsonArray()

                for (i in 0 until value.size) {
                    array.add(value[i])
                }

                result.add(key.toString(), array)
            }

            result
        }
    }
}
