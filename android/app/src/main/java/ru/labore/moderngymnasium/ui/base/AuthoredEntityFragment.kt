package ru.labore.moderngymnasium.ui.base

import androidx.fragment.app.viewModels
import ru.labore.moderngymnasium.data.db.entities.AuthoredEntity

abstract class AuthoredEntityFragment(
    controls: Companion.ListElementFragmentControls,
    val item: AuthoredEntity
) : ListElementFragment(controls) {

}