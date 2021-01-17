package ru.labore.moderngymnasium.ui.fragments.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.data.repository.AppRepository

class InboxViewModel(
    application: Application
) : AndroidViewModel(application), DIAware {
    override val di: DI by lazy { (application as DIAware).di }
    val appRepository: AppRepository by instance()

    fun cleanseUser() {
        appRepository.user = null
    }

    suspend fun getAnnouncements(offset: Int, forceFetch: Boolean? = false) =
        appRepository.getAnnouncements(offset, 25, forceFetch)
}