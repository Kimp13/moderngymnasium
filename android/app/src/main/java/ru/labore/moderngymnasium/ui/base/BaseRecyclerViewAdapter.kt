package ru.labore.moderngymnasium.ui.base

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import ru.labore.moderngymnasium.R

abstract class BaseRecyclerViewAdapter
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object Base {
        const val DEFAULT_VIEW_POSITION = -1
        const val LOADING_VIEW_HOLDER_ID = "loading"

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
    }

    val beginAdditionalItems = arrayListOf<AdditionalItem>()
    val endAdditionalItems = arrayListOf<AdditionalItem>()

    var loading: Boolean = true
        set(value) {
            if (field != value) {
                field = value

                updateAdditionalItems()
            }
        }

    protected abstract val defaultItemCount: Int
    protected abstract fun createDefaultViewHolder(
        parent: ViewGroup
    ): BaseViewHolder

    protected open fun updateAdditionalItems() {
        if (loading) {
            var absent = true

            for (i in 0 until beginAdditionalItems.size)
                if (beginAdditionalItems[i].id == LOADING_VIEW_HOLDER_ID) {
                    absent = false
                    break
                }

            if (absent) {
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
            for (i in 0 until endAdditionalItems.size) {
                if (endAdditionalItems[i].id == LOADING_VIEW_HOLDER_ID) {
                    endAdditionalItems.removeAt(i)
                    notifyItemRemoved(
                        beginAdditionalItems.size + defaultItemCount + i
                    )
                    break
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val ret = if (position < beginAdditionalItems.size) {
            position
        } else {
            val i = position - defaultItemCount - beginAdditionalItems.size

            if (i >= 0)
                i + beginAdditionalItems.size
            else
                DEFAULT_VIEW_POSITION
        }

        return ret
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            DEFAULT_VIEW_POSITION -> createDefaultViewHolder(parent)
            in 0 until beginAdditionalItems.size -> beginAdditionalItems[viewType](parent)
            else -> endAdditionalItems[viewType - beginAdditionalItems.size](parent)
        }
    }

    override fun getItemCount(): Int {
        return beginAdditionalItems.size + defaultItemCount + endAdditionalItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as BaseViewHolder).onBind(position, this)
    }
}