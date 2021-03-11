package ru.labore.eventeger.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.labore.eventeger.R
import ru.labore.eventeger.data.network.exceptions.ClientConnectionException
import ru.labore.eventeger.data.network.exceptions.ClientErrorException
import ru.labore.eventeger.ui.base.BaseActivity
import ru.labore.eventeger.utils.hideKeyboard
import java.net.ConnectException

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        apiRequestButton.setOnClickListener {
            val username = usernameField.text
            val password = passwordField.text
            val regex = Regex("[^0-9a-zA-Z\$#*_]")
            val isCorrect = when {
                username.isEmpty() -> {
                    Toast.makeText(
                        this,
                        getString(R.string.enter_username),
                        Toast.LENGTH_SHORT
                    ).show() // Уведомление о неверности данных
                    false
                }
                regex.containsMatchIn(username) -> {
                                        Toast.makeText(
                        this,
                        getString(R.string.username_incorrect),
                        Toast.LENGTH_SHORT
                    ).show() // Уведомление о неверности данных
                    false
                }
                password.isEmpty() -> {
                                        Toast.makeText(
                        this,
                        getString(R.string.enter_password),
                        Toast.LENGTH_SHORT
                    ).show() // Уведомление о неверности данных
                    false
                }
                password.length < 8 -> {
                                        Toast.makeText(
                        this,
                        getString(R.string.password_too_short),
                        Toast.LENGTH_SHORT
                    ).show() // Уведомление о неверности данных
                    false
                }
                else -> {
                    true
                }
            }

            if (isCorrect) {
                                apiRequestButton.isEnabled = false

                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        repository.signIn(username.toString(), password.toString())

                        startActivity(
                            Intent(
                                this@LoginActivity,
                                MainActivity::class.java
                            )
                        )

                        finish()
                    } catch (e: Exception) {
                        println(e.message)

                        val toastString: String = when (e) {
                            is ConnectException -> getString(R.string.server_unavailable)
                            is ClientConnectionException -> getString(R.string.no_internet)
                            is ClientErrorException -> getString(R.string.invalid_credentials)
                            else -> "An unknown error occurred"
                        }

                        apiRequestButton.isEnabled = true

                        Toast.makeText(
                            this@LoginActivity,
                            toastString,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        rootLoginLayout.setOnClickListener { hideKeyboard() }
    }
}