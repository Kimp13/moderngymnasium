package ru.labore.moderngymnasium.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import kotlinx.android.synthetic.main.activity_main.*
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.adapters.MainFragmentPagerAdapter
import ru.labore.moderngymnasium.ui.create.CreateFragment

class MainActivity : FragmentActivity(), DIAware {
    override val di: DI by lazy { (applicationContext as DIAware).di }

    private lateinit var viewPagerAdapter: MainFragmentPagerAdapter
    private var inboxBadge: BadgeDrawable? = null
    private val repository: AppRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (repository.user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            setActionBar(toolbar)

            viewPagerAdapter = MainFragmentPagerAdapter(supportFragmentManager, lifecycle)
            navHostFragment.adapter = viewPagerAdapter
            navHostFragment.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    bottomNav.selectedItemId = bottomNav.menu.getItem(position).itemId
                }
            })

            bottomNav.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_SELECTED
            inboxBadge = bottomNav.getOrCreateBadge(bottomNav.menu.getItem(0).itemId)
            inboxBadge?.isVisible = false

            bottomNav.setOnNavigationItemSelectedListener {
                loadFragment(it)
            }

            bottomNav.setOnNavigationItemReselectedListener {
                loadFragment(it)
            }
        }
    }

    fun updateInboxBadge(newNumber: Int) {
        inboxBadge?.apply {
            isVisible = newNumber > 0
            number = newNumber
        }
    }

    fun revealCreateFragment(x: Int, y: Int, radius: Float) {
        val fragment = supportFragmentManager.findFragmentByTag("create_fragment")
            as CreateFragment

        fragment.reveal(x, y, radius)
    }

    private fun loadFragment(menuItem: MenuItem): Boolean {
        for (i in 0 until viewPagerAdapter.itemCount) {
            if (bottomNav.menu.getItem(i).itemId == menuItem.itemId) {
                navHostFragment.currentItem = i
                return true
            }
        }

        return false
    }
}