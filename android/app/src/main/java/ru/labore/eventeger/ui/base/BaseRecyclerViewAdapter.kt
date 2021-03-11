package ru.labore.eventeger.ui.base

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import ru.labore.eventeger.R
import ru.labore.eventeger.data.db.entities.AuthoredEntity
import kotlin.math.min

abstract class BaseRecyclerViewAdapter(
    open val viewModel: BaseRecyclerViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object Base {
        const val DEFAULT_VIEW_POSITION = -1
        const val LOADING_VIEW_HOLDER_ID = "loading"
        const val NOTHING_VIEW_HOLDER_ID = "nothing"

        class AdditionalItem(
            val id: String,
            private val create: (ViewGroup) -> BaseViewHolder
        ) {
            override fun equals(other: Any?): Boolean {
                return if (other is AdditionalItem) {
                    other.id == id
                } else {
                    false
                }
            }

            override fun hashCode(): Int {
                return id.hashCode()
            }

            operator fun invoke(viewGroup: ViewGroup): BaseViewHolder {
                return create(viewGroup)
            }
        }

        class LoadingViewHolder(layout: LinearLayout) : BaseViewHolder(layout) {
            override fun onBind(position: Int, parent: BaseRecyclerViewAdapter) {

            }
        }

        class NothingViewHolder(layout: LinearLayout) : BaseViewHolder(layout) {
            override fun onBind(position: Int, parent: BaseRecyclerViewAdapter) {

            }
        }
    }

    val beginAdditionalItems = mutableListOf<AdditionalItem>()
    private val endAdditionalItems = mutableListOf<AdditionalItem>()

    var loading: Boolean = true
        set(value) {
            field = value

            updateAdditionalItems()
        }

    protected abstract val defaultItemCount: Int
    protected abstract fun createDefaultViewHolder(
        parent: ViewGroup
    ): BaseViewHolder

    protected open fun updateAdditionalItems() {
        if (loading) {
            var loadingAbsent = true

            endAdditionalItems.forEachIndexed { i, it ->
                when (it.id) {
                    LOADING_VIEW_HOLDER_ID -> loadingAbsent = false
                    NOTHING_VIEW_HOLDER_ID -> {
                        endAdditionalItems.removeAt(i)

                        notifyItemRemoved(beginAdditionalItems.size + defaultItemCount + i)
                    }
                }
            }

            if (loadingAbsent) {
                endAdditionalItems.add(
                    AdditionalItem(
                        LOADING_VIEW_HOLDER_ID
                    ) {
                        LoadingViewHolder(
                            LayoutInflater.from(it.context)
                                .inflate(
                                    R.layout.loading_view_holder,
                                    it,
                                    false
                                ) as LinearLayout
                        )
                    }
                )

                notifyItemInserted(itemCount - 1)
            }
        } else {
            var nothingAbsent = true

            endAdditionalItems.forEachIndexed { i, it ->
                when (it.id) {
                    NOTHING_VIEW_HOLDER_ID -> nothingAbsent = false
                    LOADING_VIEW_HOLDER_ID -> {
                        endAdditionalItems.removeAt(i)

                        notifyItemRemoved(
                            beginAdditionalItems.size + defaultItemCount + i
                        )

                        println("$beginAdditionalItems")
                        println("$endAdditionalItems")
                    }
                }
            }

            if (nothingAbsent && defaultItemCount == 0) {
                endAdditionalItems.add(
                    AdditionalItem(
                        NOTHING_VIEW_HOLDER_ID
                    ) {
                        NothingViewHolder(
                            LayoutInflater.from(it.context)
                                .inflate(
                                    R.layout.nothing_view_holder,
                                    it,
                                    false
                                ) as LinearLayout
                        )
                    }
                )

                notifyItemInserted(itemCount - 1)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < beginAdditionalItems.size) {
            position
        } else {
            val i = position - defaultItemCount - beginAdditionalItems.size

            if (i >= 0)
                i + beginAdditionalItems.size
            else
                DEFAULT_VIEW_POSITION
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        println("$viewType")
        return when (viewType) {
            DEFAULT_VIEW_POSITION -> createDefaultViewHolder(parent)
            in 0 until beginAdditionalItems.size -> {
                println("They're asking for begin! $viewType")
                beginAdditionalItems[viewType](parent)
            }
            else -> endAdditionalItems[viewType - beginAdditionalItems.size](parent)
        }
    }

    override fun getItemCount(): Int {
        return beginAdditionalItems.size + defaultItemCount + endAdditionalItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as BaseViewHolder).onBind(position, this)
    }

    fun prependItem() {
        notifyItemInserted(beginAdditionalItems.size)
    }

    fun prependItem(item: AuthoredEntity) {
        viewModel.items.add(0, item)

        prependItem()
    }

    fun refreshItems(
        previousSize: Int,
        newSize: Int
    ) {
        if (previousSize > newSize) {
            notifyItemRangeRemoved(
                beginAdditionalItems.size + newSize,
                previousSize - newSize
            )
        }

        val minimal = min(newSize, previousSize)

        if (minimal > 0)
            notifyItemRangeChanged(
                beginAdditionalItems.size,
                minimal
            )

        if (previousSize < newSize) {
            notifyItemRangeInserted(
                beginAdditionalItems.size + previousSize,
                newSize - previousSize
            )
        }
    }

    fun pushItems(
        previousSize: Int,
        addedSize: Int
    ) {
        notifyItemRangeInserted(
            previousSize + beginAdditionalItems.size,
            addedSize
        )
    }
}