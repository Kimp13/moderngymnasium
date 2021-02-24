package ru.labore.moderngymnasium.ui.base

import android.app.Application
import kotlinx.coroutines.Job
import ru.labore.moderngymnasium.data.db.entities.AuthoredEntity

abstract class BaseRecyclerViewModel(
    val app: Application
) : BaseViewModel(app) {
    protected lateinit var adapter: BaseRecyclerViewAdapter
    protected var currentOffset = 0
    protected var current: Job? = null
    protected var reachedEnd = false

    protected var loading: Boolean
        get() =
            adapter.loading
        set(value) {
            adapter.loading = value
        }

    val items = mutableListOf<AuthoredEntity>()

    val itemCount
        get() = items.size
}