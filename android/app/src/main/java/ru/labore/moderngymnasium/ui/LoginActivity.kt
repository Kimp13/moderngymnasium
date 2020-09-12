package ru.labore.moderngymnasium.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.network.AppNetwork
import ru.labore.moderngymnasium.data.network.ClientConnectionException
import ru.labore.moderngymnasium.data.network.ClientErrorException
import ru.labore.moderngymnasium.utils.hideKeyboard
import java.net.ConnectException

class LoginActivity : AppCompatActivity() {
    private lateinit var network: AppNetwork

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        network = AppNetwork(this)

        apiRequestButton.setOnClickListener {
            val username = usernameField.text
            val password = passwordField.text
            val regex = Regex("[^0-9a-zA-Z\$#*_]")
            var isCorrect = true

            if (username.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_username), Toast.LENGTH_SHORT).show()
                isCorrect = false
            } else if (regex.containsMatchIn(username)) {
                Toast.makeText(this, getString(R.string.username_incorrect), Toast.LENGTH_SHORT).show()
                isCorrect = false
            } else if (password.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show()
                isCorrect = false
            } else if (password.length < 8) {
                Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
                isCorrect = false
            }

            if (isCorrect) {
                apiRequestButton.isEnabled = false

                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        val user = network.signUserIn(username.toString(), password.toString())

                        val editor = this@LoginActivity.getSharedPreferences(
                            getString(R.string.utility_shared_preference_file_key),
                            Context.MODE_PRIVATE
                        ).edit()

                        editor.putString("user", Gson().toJson(user))

                        editor.apply()

                        startActivity(Intent(
                                this@LoginActivity,
                                MainActivity::class.java
                            )
                        )

                        finish()
                    } catch (e: Exception) {
                        val toastString: String = when (e) {
                            is ConnectException -> getString(R.string.server_unavailable)
                            is ClientConnectionException -> getString(R.string.no_internet)
                            is ClientErrorException -> getString(R.string.invalid_credentials)
                            else -> throw e
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

        rootLayout.setOnClickListener {hideKeyboard()}
    }
}