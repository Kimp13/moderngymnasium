package ru.labore.moderngymnasium.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.labore.moderngymnasium.ui.create.MenuCreateFragment
import ru.labore.moderngymnasium.ui.inbox.MenuInboxFragment
import ru.labore.moderngymnasium.ui.profile.MenuProfileFragment

class MainFragmentPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) :
    FragmentStateAdapter(fm, lifecycle) {
    companion object {
        private val BASE_FRAGMENTS = arrayOf(
            MenuCreateFragment(),
            MenuInboxFragment(),
            MenuProfileFragment()
        )

        private const val CREATE_POSITION = 0L
        private const val INBOX_POSITION = 1L
        private const val PROFILE_POSITION = 2L
    }

    private val createFragments = arrayListOf<Fragment>()
    private val inboxFragments = arrayListOf<Fragment>()
    private val profileFragments = arrayListOf<Fragment>()

    private fun getItem(position: Int): Fragment {
        when (position.toLong()) {
            CREATE_POSITION -> {
                return if (createFragments.isEmpty()) {
                    BASE_FRAGMENTS[position]
                } else {
                    createFragments.last()
                }
            }

            INBOX_POSITION -> {
                return if (inboxFragments.isEmpty()) {
                    BASE_FRAGMENTS[position]
                } else {
                    inboxFragments.last()
                }
            }

            else -> {
                return if (profileFragments.isEmpty()) {
                    BASE_FRAGMENTS[position]
                } else {
                    profileFragments.last()
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        if (
            getItem(position) == BASE_FRAGMENTS[position]
        ) {
            return when (position.toLong()) {
                CREATE_POSITION -> CREATE_POSITION
                INBOX_POSITION -> INBOX_POSITION
                PROFILE_POSITION -> PROFILE_POSITION
                else -> getItem(position).hashCode().toLong()
            }
        }

        return getItem(position).hashCode().toLong()
    }

    override fun getItemCount() = BASE_FRAGMENTS.size

    override fun createFragment(position: Int) = getItem(position)
}
