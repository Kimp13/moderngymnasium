package ru.labore.moderngymnasium.data.sharedpreferences.entities
import com.google.gson.*

abstract class Permissions {
    val all: Boolean

    constructor(wildcard: Boolean) {
        all = wildcard
    }
}

class ActionPermissionsTargets: Permissions {
    val contents: Array<Int>
    val size: Int
        get() = contents.size

    constructor(wildcard: Boolean) : super(wildcard) {
        contents = arrayOf()
    }

    constructor(targets: Array<Int>) : super(false) {
        contents = targets
    }

    operator fun get(index: Int) =
        contents[index]

    fun serialize (): JsonElement = when {
        all -> JsonPrimitive(true)
        size == 0 -> JsonPrimitive(false)
        else -> {
            val result = JsonArray()

            for (i in 0 until size) {
                result.add(contents[i])
            }

            result
        }
    }
}

class OperationPermissions: Permissions {
    private val attributes = HashMap<String, ActionPermissionsTargets>()
    val size: Int
        get() = attributes.size

    constructor (wildcard: Boolean): super(wildcard)

    constructor (json: JsonObject): super(false) {
        val entrySet: Set<Map.Entry<String?, JsonElement?>> = json.entrySet()
        for ((key, value) in entrySet) {
            if (key != null) {
                if (value?.isJsonArray == true) {
                    val array = value.asJsonArray
                    attributes[key] = ActionPermissionsTargets(Array(
                        array.size()
                    ) {
                        array.get(it).asInt
                    })
                } else {
                    attributes[key] = ActionPermissionsTargets(value?.asBoolean == true)
                }
            }
        }
    }

    operator fun get(key: String): ActionPermissionsTargets =
        if (all) {
            ActionPermissionsTargets(true)
        } else {
            attributes[key] ?: ActionPermissionsTargets(false)
        }

    fun serialize (): JsonElement = when {
        all -> JsonPrimitive(true)
        size == 0 -> JsonPrimitive(false)
        else -> {
            val result = JsonObject()

            for ((key, value) in attributes) {
                result.add(key, value.serialize())
            }

            result
        }
    }
}

class AllPermissions: Permissions {
    private val attributes = HashMap<String, OperationPermissions>()
    val size: Int
        get() = attributes.size

    constructor (wildcard: Boolean): super(wildcard)

    constructor (json: JsonObject): super(false) {
        val entrySet: Set<Map.Entry<String?, JsonElement?>> = json.entrySet()
        for ((key, value) in entrySet) {
            if (key != null) {
                if (value?.isJsonObject == true) {
                    val obj = value.asJsonObject
                    attributes[key] = OperationPermissions(obj)
                } else {
                    attributes[key] = OperationPermissions(value?.asBoolean == true)
                }
            }
        }
    }

    operator fun get(key: String): OperationPermissions =
        if (all) {
            OperationPermissions(true)
        } else {
            attributes[key] ?: OperationPermissions(false)
        }

    fun serialize (): JsonElement = when {
        all -> JsonPrimitive(true)
        size == 0 -> JsonPrimitive(false)
        else -> {
            val result = JsonObject()

            for ((key, value) in attributes) {
                result.add(key, value.serialize())
            }

            result
        }
    }
}

data class UserData(
    val firstName: String? = null,
    val lastName: String? = null,
    val classId: Int? = null,
    val roleId: Int? = null,

    val username: String,
    val permissions: AllPermissions = AllPermissions(false)
)

data class User(
    val jwt: String,
    val data: UserData
)