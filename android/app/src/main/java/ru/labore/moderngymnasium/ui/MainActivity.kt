package ru.labore.moderngymnasium.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.user.User

class MainActivity : AppCompatActivity() {
    private lateinit var navController : NavController
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userString = getSharedPreferences(
            getString(R.string.utility_shared_preference_file_key),
            Context.MODE_PRIVATE
        ).getString("user", null)

        if (userString == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Working with user
            user = Gson().fromJson(userString, User::class.java)
            Toast.makeText(this, user.toString(), Toast.LENGTH_SHORT).show()

            // Navigation Setup
            navController = Navigation.findNavController(this, R.id.navHostFragment)
            setSupportActionBar(toolbar)
            bottomNav.setupWithNavController(navController)
            NavigationUI.setupActionBarWithNavController(this, navController)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}