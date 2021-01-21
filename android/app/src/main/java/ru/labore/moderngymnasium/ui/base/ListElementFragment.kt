package ru.labore.moderngymnasium.ui.base

import androidx.fragment.app.Fragment

abstract class ListElementFragment(
    push: (Fragment) -> Unit,
    finish: () -> Unit
) : ScopedFragment() {
}