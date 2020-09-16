package ru.labore.moderngymnasium.ui.inbox

import androidx.lifecycle.ViewModel
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.utils.lazyDeferred

class MenuInboxViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    val announcements by lazyDeferred {
        appRepository.getAnnouncements()
    }
}