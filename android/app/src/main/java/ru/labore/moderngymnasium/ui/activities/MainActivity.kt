package ru.labore.moderngymnasium.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_main.*
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.adapters.MainFragmentPagerAdapter
import ru.labore.moderngymnasium.utils.hideKeyboard

class MainActivity : AppCompatActivity(), DIAware {
    override val di: DI by lazy { (applicationContext as DIAware).di }

    private lateinit var viewPagerAdapter: MainFragmentPagerAdapter
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
            setSupportActionBar(toolbar)

            viewPagerAdapter = MainFragmentPagerAdapter(supportFragmentManager, lifecycle)
            navHostFragment.adapter = viewPagerAdapter
            
            navHostFragment.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    bottomNav.selectedItemId = bottomNav.menu.getItem(position).itemId
                }
            })

            bottomNav.setOnNavigationItemSelectedListener {
                loadFragment(it)
            }

            bottomNav.setOnNavigationItemReselectedListener {
                loadFragment(it)
            }
        }

        rootMainLayout.setOnClickListener {hideKeyboard()}
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