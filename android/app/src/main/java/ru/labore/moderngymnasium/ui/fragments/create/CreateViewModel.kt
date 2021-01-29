package ru.labore.moderngymnasium.ui.fragments.create

import android.app.Application
import ru.labore.moderngymnasium.ui.base.BaseViewModel

class CreateViewModel(
    application: Application
) : BaseViewModel(application) {
    val checkedRoles: HashMap<Int, HashSet<Int>> = hashMapOf()

    suspend fun createAnnouncement(text: String) =
        appRepository.createAnnouncement(text, checkedRoles)

    suspend fun getRoles() = appRepository.getKeyedRoles(
        appRepository.announceMap.rolesIds
    )

    suspend fun getClasses() = appRepository.getKeyedClasses(
        appRepository.announceMap.classesIds
    )
}