package ru.labore.eventeger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import ru.labore.eventeger.R
import ru.labore.eventeger.ui.base.BaseActivity
import ru.labore.eventeger.utils.setupWithNavController

class MainActivity : BaseActivity() {
    private var currentNavController: LiveData<NavController>? = null

    private var inboxBadge: BadgeDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (repository.user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else if (savedInstanceState == null) {
            bottomNav.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
            inboxBadge = bottomNav.getOrCreateBadge(bottomNav.menu.getItem(1).itemId)
            inboxBadge?.isVisible = false

            setupBottomNavigationBar()

            launch {
                makeRequest({
                    repository.onMainActivityCreated()
                })
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        setupBottomNavigationBar()
    }

    private fun setupBottomNavigationBar() {
        val navGraphIds = listOf(R.navigation.news, R.navigation.inbox)

        val controller = bottomNav.setupWithNavController(
            navGraphIds,
            supportFragmentManager,
            R.id.mainFragmentContainer,
            intent
        )

        bottomNav.selectedItemId = R.id.inbox

        controller.observe(this, {
            setupActionBarWithNavController(it)
        })

        currentNavController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    fun updateInboxBadge(newNumber: Int) {
        inboxBadge?.apply {
            isVisible = newNumber > 0
            number = newNumber
        }
    }
}