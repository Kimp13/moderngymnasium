package ru.labore.moderngymnasium.ui.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.utils.lazyDeferred

class MenuCreateViewModel(
    application: Application
) : AndroidViewModel(application), DIAware {
    override val di: DI by lazy { (application as DIAware).di }
    private val appRepository: AppRepository by instance()

    val roles by lazyDeferred {
        appRepository.getUserRoles().filterNotNull()
    }

    val classes by lazyDeferred {
        appRepository.getUserClasses()
    }

    suspend fun createAnnouncement(text: String, recipients: HashMap<Int, MutableList<Int>>) =
        appRepository.createAnnouncement(text, recipients)
}