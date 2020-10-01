package ru.labore.moderngymnasium.data.sharedpreferences.entities
import com.google.gson.annotations.SerializedName

data class ActionPermissions(
    val create: Array<Int>? = null,
    val read: Array<Int>? = null,
    val update: Array<Int>? = null,
    val delete: Array<Int>? = null,
    val comment: Array<Int>? = null
)

data class AllPermissions(
    @SerializedName("*")
    val all: Boolean? = null,
    val announcement: ActionPermissions? = null,
    val profile: ActionPermissions? = null
)

data class UserData(
    @SerializedName("first_name")
    val firstName: String? = null,

    @SerializedName("last_name")
    val lastName: String? = null,

    @SerializedName("class_id")
    val classId: Int? = null,

    @SerializedName("role_id")
    val roleId: Int? = null,

    val username: String,
    val permissions: AllPermissions? = null
)

data class User(
    val jwt: String,
    val data: UserData
)