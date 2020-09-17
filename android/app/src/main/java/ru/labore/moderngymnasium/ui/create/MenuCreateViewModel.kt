package ru.labore.moderngymnasium.ui.create

import androidx.lifecycle.ViewModel
import ru.labore.moderngymnasium.data.repository.AppRepository

class MenuCreateViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    suspend fun createAnnouncement(text: String) {
        appRepository.createAnnouncement(text)
    }
}