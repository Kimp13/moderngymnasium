package ru.labore.moderngymnasium.ui

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.utils.hideKeyboard

class MainActivity : AppCompatActivity(), DIAware {
    override val di: DI by lazy { (applicationContext as DIAware).di }

    private lateinit var navController : NavController
    private val repository: AppRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (repository.user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, repository.user.toString(), Toast.LENGTH_SHORT).show()

            // Navigation Setup
            navController = Navigation.findNavController(this, R.id.navHostFragment)
            setSupportActionBar(toolbar)
            bottomNav.setupWithNavController(navController)
            NavigationUI.setupActionBarWithNavController(this, navController)
        }

        rootMainLayout.setOnClickListener {hideKeyboard()}
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}