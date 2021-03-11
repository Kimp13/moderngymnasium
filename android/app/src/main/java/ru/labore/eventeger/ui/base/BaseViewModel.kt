package ru.labore.eventeger.ui.base

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.eventeger.R
import ru.labore.eventeger.data.AppRepository
import ru.labore.eventeger.data.network.exceptions.ClientConnectionException
import ru.labore.eventeger.data.network.exceptions.ClientErrorException
import ru.labore.eventeger.ui.activities.LoginActivity
import java.net.ConnectException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel(
    application: Application
) : AndroidViewModel(application), DIAware {
    override val di: DI by lazy { (application as DIAware).di }
    val appRepository: AppRepository by instance()

    fun cleanseUser() {
        appRepository.user = null
    }

    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(context, start, block)
    }

    protected suspend fun makeRequest(
        activity: Activity,
        toTry: suspend () -> Unit,
        whenCaught: suspend () -> Unit = {}
    ) {
        try {
            toTry()
        } catch (e: Exception) {
            println(e.toString())

            whenCaught()

            MainScope().launch {
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
            }
        }
    }
}