package ru.labore.moderngymnasium.ui.base

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.network.ClientConnectionException
import ru.labore.moderngymnasium.data.network.ClientErrorException
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.activities.LoginActivity
import java.net.ConnectException

abstract class BaseViewModel(
    application: Application
) : AndroidViewModel(application), DIAware {
    override val di: DI by lazy { (application as DIAware).di }
    val appRepository: AppRepository by instance()

    fun cleanseUser() {
        appRepository.user = null
    }

    protected suspend fun makeRequest(
        activity: Activity,
        toTry: suspend () -> Unit,
        whenCaught: suspend () -> Unit = {}
    ) {
        try {
            toTry()
        } catch(e: Exception) {
            Toast.makeText(
                activity,
                when (e) {
                    is ConnectException ->
                        activity.getString(R.string.server_unavailable)
                    is ClientConnectionException ->
                        activity.getString(R.string.no_internet)
                    is ClientErrorException -> {
                        if (e.errorCode == AppRepository.HTTP_RESPONSE_CODE_UNAUTHORIZED) {
                            cleanseUser()

                            activity.startActivity(
                                Intent(
                                    activity,
                                    LoginActivity::class.java
                                )
                            )
                            activity.finish()

                            activity.getString(R.string.invalid_credentials)
                        } else {
                            println(e.toString())
                            "An unknown error occurred"
                        }
                    }
                    else -> {
                        println(e.toString())
                        "An unknown error occurred."
                    }
                },
                Toast.LENGTH_LONG
            ).show()

            whenCaught()
        }
    }
}