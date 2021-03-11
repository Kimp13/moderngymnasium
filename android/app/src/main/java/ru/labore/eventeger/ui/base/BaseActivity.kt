package ru.labore.eventeger.ui.base

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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

abstract class BaseActivity : AppCompatActivity(), CoroutineScope, DIAware {
    override val di: DI by lazy { (applicationContext as DIAware).di }

    protected val repository: AppRepository by instance()
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        job = Job()


    }

    override fun onDestroy() {
        super.onDestroy()

        job.cancel()
    }

    protected fun makeRequest(
        toTry: suspend () -> Unit,
        whenCaught: suspend () -> Unit = {}
    ) = launch {
        try {
            toTry()
        } catch(e: Exception) {
            Toast.makeText(
                this@BaseActivity,
                when (e) {
                    is ConnectException -> getString(R.string.server_unavailable)
                    is ClientConnectionException -> getString(R.string.no_internet)
                    is ClientErrorException -> {
                        if (e.errorCode == AppRepository.HTTP_RESPONSE_CODE_UNAUTHORIZED) {
                            repository.user = null

                            startActivity(
                                Intent(
                                    this@BaseActivity,
                                    LoginActivity::class.java
                                )
                            )
                            finish()

                            getString(R.string.invalid_credentials)
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