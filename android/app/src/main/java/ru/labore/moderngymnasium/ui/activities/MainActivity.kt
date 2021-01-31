package ru.labore.moderngymnasium.ui.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.network.ClientConnectionException
import ru.labore.moderngymnasium.data.network.ClientErrorException
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.base.BaseActivity
import ru.labore.moderngymnasium.ui.base.ListElementFragment
import ru.labore.moderngymnasium.ui.fragments.inbox.InboxFragment
import ru.labore.moderngymnasium.ui.fragments.news.NewsFragment
import java.net.ConnectException

class MainActivity : BaseActivity() {
    private val rootFragments = arrayOf<Fragment>(
        NewsFragment(ListElementFragment.Companion.ListElementFragmentControls(
            pushFragment(0),
            dropFragment(0),
            { showBottomNav() },
            { hideBottomNav() }
        )),
        InboxFragment(ListElementFragment.Companion.ListElementFragmentControls(
            pushFragment(1),
            dropFragment(1),
            { showBottomNav() },
            { hideBottomNav() }
        ))
    )

    private var inboxBadge: BadgeDrawable? = null
    private val menuItemIdToIndex = HashMap<Int, Int>()
    private val animator = ObjectAnimator()
    private var hidden = false
    private val fragments = Array<ArrayList<Fragment>>(rootFragments.size) {
        arrayListOf(rootFragments[it])
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        animator.target = bottomNav
        animator.duration = 400
        animator.setPropertyName("translationY")

        if (repository.user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            bottomNav.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
            inboxBadge = bottomNav.getOrCreateBadge(bottomNav.menu.getItem(0).itemId)
            inboxBadge?.isVisible = false

            for (i in 0 until bottomNav.menu.children.count()) {
                menuItemIdToIndex[bottomNav.menu[i].itemId] = i
            }

            bottomNav.setOnNavigationItemSelectedListener {
                loadFragment(it)
            }

            bottomNav.setOnNavigationItemReselectedListener {
                setRootFragment(it)
            }

            launch {
                try {
                    repository.onMainActivityCreated()
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext,
                        when (e) {
                            is ConnectException ->
                                getString(R.string.server_unavailable)
                            is ClientConnectionException ->
                                getString(R.string.no_internet)
                            is ClientErrorException -> {
                                if (
                                    e.errorCode ==
                                    AppRepository.HTTP_RESPONSE_CODE_UNAUTHORIZED
                                ) {
                                    repository.user = null
                                    val intent = Intent(
                                        applicationContext,
                                        LoginActivity::class.java
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    startActivity(intent)

                                    getString(R.string.session_timed_out)
                                } else {
                                    println(e.toString())
                                    "An unknown error has occurred."
                                }
                            }
                            else -> {
                                println(e.toString())
                                "An unknown error has occurred."
                            }
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun updateInboxBadge(newNumber: Int) {
        inboxBadge?.apply {
            isVisible = newNumber > 0
            number = newNumber
        }
    }

    private fun dropFragment(index: Int): () -> Unit = {
        fragments[index].removeLast()
        loadFragment(index)
    }

    private fun pushFragment(index: Int): (Fragment) -> Unit = {
        fragments[index].add(it)
        loadFragment(index)
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

    private fun showBottomNav() {
        if (hidden) {
            hidden = false

            val displayHeight =
                if (
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
                ) {
                    windowManager.currentWindowMetrics.bounds.height()
                } else {
                    val metrics: DisplayMetrics = DisplayMetrics()

                    windowManager.defaultDisplay.getMetrics(metrics)

                    metrics.heightPixels
                }

            val out = TypedValue()
            resources.getValue(R.dimen.bottom_nav_vertical_bias, out, true)

            val yDelta = (1 - out.float) * displayHeight * 3;
            val multiplier: Float

            if (animator.isRunning) {
                multiplier =
                    animator.currentPlayTime.toFloat() / animator.duration.toFloat()

                animator.end()
            } else {
                multiplier = 1f
            }

            animator.setFloatValues(yDelta * multiplier, 0F)
            animator.start()
        }
    }

    private fun hideBottomNav() {
        if (!hidden) {
            hidden = true
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
            val multiplier: Float

            if (animator.isRunning) {
                multiplier =
                    animator.currentPlayTime.toFloat() / animator.duration.toFloat()

                animator.end()
            } else {
                multiplier = 1f
            }

            animator.setFloatValues(yDelta - (yDelta * multiplier), yDelta)
            animator.start()
        }
    }
}