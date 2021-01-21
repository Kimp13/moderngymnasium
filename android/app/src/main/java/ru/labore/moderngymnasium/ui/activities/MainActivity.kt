package ru.labore.moderngymnasium.ui.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MenuItem
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.base.ScopedActivity
import ru.labore.moderngymnasium.ui.create.CreateFragment
import ru.labore.moderngymnasium.ui.fragments.inbox.InboxFragment
import ru.labore.moderngymnasium.ui.fragments.news.NewsFragment
import ru.labore.moderngymnasium.ui.fragments.profile.ProfileFragment
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

class MainActivity : ScopedActivity(), DIAware {
    private val rootFragments = arrayOf<Fragment>(
        NewsFragment(pushFragment(0), dropFragment(0)),
        InboxFragment(pushFragment(1), dropFragment(1)),
        ProfileFragment(pushFragment(2), dropFragment(2))
    )

    override val di: DI by lazy { (applicationContext as DIAware).di }

    private var inboxBadge: BadgeDrawable? = null
    private val repository: AppRepository by instance()
    private val menuItemIdToIndex = HashMap<Int, Int>()
    private val fragments = Array<ArrayList<Fragment>>(rootFragments.size) {
        arrayListOf(rootFragments[it])
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (repository.user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            bottomNav.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
            inboxBadge = bottomNav.getOrCreateBadge(bottomNav.menu.getItem(0).itemId)
            inboxBadge?.isVisible = false

            val childrenCount = bottomNav.menu.children.count()

            for (i in 0 until bottomNav.menu.children.count()) {
                menuItemIdToIndex[bottomNav.menu[i].itemId] = i
            }

            bottomNav.setOnNavigationItemSelectedListener {
                loadFragment(it)
            }

            bottomNav.setOnNavigationItemReselectedListener {
                val displayHeight =
                    if (
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
                        ) {
                        windowManager.currentWindowMetrics.bounds.height()
                    } else {
                        var metrics: DisplayMetrics = DisplayMetrics()

                        windowManager.defaultDisplay.getMetrics(metrics)

                        metrics.heightPixels
                    }

                val out = TypedValue()
                resources.getValue(R.dimen.bottom_nav_vertical_bias, out, true)

                val yDelta = (1 - out.float) * displayHeight * 3;

                val animator = ObjectAnimator.ofFloat(
                    bottomNav,
                    "translationY",
                    0F,
                    yDelta
                )
                animator.duration = 400;
                animator.start()

                launch {
                    delay(1000)

                    val multiplier = if (animator.isRunning) {
                        animator.currentPlayTime.toFloat() / animator.duration.toFloat()
                    } else {
                        1F
                    }

                    animator.end()

                    animator.setFloatValues(yDelta * multiplier, 0F)

                    animator.start()
                }

                setRootFragment(it)
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
        val fragment = supportFragmentManager.findFragmentById(R.id.createFragment)
            as CreateFragment

        fragment.reveal(x, y, radius)
    }

    private fun dropFragment(index: Int): () -> Unit = {
        fragments[index].removeLast()
    }

    private fun pushFragment(index: Int): (Fragment) -> Unit = {
        fragments[index].add(it)
    }

    private fun loadFragment(index: Int) {
        val ft = supportFragmentManager.beginTransaction()

        bottomNav.menu[index].isChecked = true

        ft.replace(
            R.id.mainFragmentContainer,
            fragments[index].last()
        )

        ft.commit()
    }

    private fun loadFragment(menuItem: MenuItem): Boolean {
        val index = menuItemIdToIndex[menuItem.itemId]

        if (index != null) {
            loadFragment(index)
        }

        return false
    }

    private fun setRootFragment(menuItem: MenuItem): Boolean {
        val index = menuItemIdToIndex[menuItem.itemId]

        if (index != null) {
            fragments[index] = arrayListOf(rootFragments[index])
            loadFragment(index)
        }

        return false
    }
}