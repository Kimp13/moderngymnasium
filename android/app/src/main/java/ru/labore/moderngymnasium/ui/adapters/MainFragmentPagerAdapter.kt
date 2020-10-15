package ru.labore.moderngymnasium.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.labore.moderngymnasium.ui.inbox.MenuInboxFragment
import ru.labore.moderngymnasium.ui.profile.MenuProfileFragment

class MainFragmentPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) :
    FragmentStateAdapter(fm, lifecycle) {
    companion object {
        private val BASE_FRAGMENTS = arrayOf(
            MenuInboxFragment(),
            MenuProfileFragment()
        )

        private const val INBOX_POSITION = 0L
        private const val PROFILE_POSITION = 1L
    }

    private val inboxFragments = arrayListOf<Fragment>()
    private val profileFragments = arrayListOf<Fragment>()

    private fun getItem(position: Int): Fragment {
        when (position.toLong()) {
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
