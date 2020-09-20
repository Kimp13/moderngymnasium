package ru.labore.moderngymnasium.ui.create

import androidx.lifecycle.ViewModel
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.utils.lazyDeferred

class MenuCreateViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    val roles by lazyDeferred {
        appRepository.getUserRoles().filterNotNull()
    }

    suspend fun createAnnouncement(text: String, recipients: Array<Int>) {
        appRepository.createAnnouncement(text, recipients)
    }
}