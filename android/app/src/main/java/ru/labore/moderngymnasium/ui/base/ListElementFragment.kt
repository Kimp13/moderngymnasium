package ru.labore.moderngymnasium.ui.base

import androidx.fragment.app.Fragment

abstract class ListElementFragment(
    val controls: ListElementFragmentControls
) : BaseFragment() {
    companion object {
        data class ListElementFragmentControls(
            val push: (ListElementFragment) -> Unit,
            val finish: () -> Unit,
            val showBottomNav: () -> Unit,
            val hideBottomNav: () -> Unit
        )
    }
}