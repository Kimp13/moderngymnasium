package ru.labore.moderngymnasium.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.labore.moderngymnasium.data.repository.AppRepository

class MenuCreateViewModelFactory (
    private val appRepository: AppRepository
): ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MenuCreateViewModel(appRepository) as T
    }
}