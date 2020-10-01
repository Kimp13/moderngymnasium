package ru.labore.moderngymnasium.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.utils.lazyDeferred

class MenuInboxViewModel(
    application: Application
) : AndroidViewModel(application), DIAware {
    override val di: DI by lazy { (application as DIAware).di }
    val appRepository: AppRepository by instance()

    val announcements by lazyDeferred {
        appRepository.getAnnouncements()
    }

    suspend fun getAnnouncements(offset: Int) = appRepository.getAnnouncements(offset)
}