package ru.labore.moderngymnasium.ui.base

import androidx.fragment.app.Fragment

open class ListElementFragment(
    push: (Fragment) -> Unit,
    finish: () -> Unit
) : ScopedFragment() {

}