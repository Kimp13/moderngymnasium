package ru.labore.moderngymnasium.ui.views

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children

class LabelledCheckbox(
    context: Context,
    labelText: String,
    specialLayoutParams: ViewGroup.LayoutParams? = null
) : LinearLayout(context) {
    private val checkbox: CheckBox
    private val label: TextView
    var checkedChangeHandler: ((Boolean) -> Unit)? = null
    var isChecked: Boolean = true
        set(value) {
            field = value
            checkbox.isChecked = value
        }

    init {
        layoutParams = specialLayoutParams
            ?: LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )

        orientation = HORIZONTAL

        label = TextView(context)
        checkbox = CheckBox(context)
        label.layoutParams = LayoutParams(
            0,
            LayoutParams.WRAP_CONTENT,
            1F
        )
        checkbox.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            0F
        )

        label.text = labelText

        addView(label)
        addView(checkbox)

        checkbox.setOnClickListener { checkbox ->
            checkedChangeHandler?.invoke((checkbox as CheckBox).isChecked)
        }
    }
}

class CheckboxLinearLayout(
    context: Context,
    private val parent: ParentCheckbox
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        setPadding(40, 10, 0, 10)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        parent.onViewToLayoutAdded(child)
    }
}

class ParentCheckbox(
    context: Context,
    labelText: String,
    specialLayoutParams: ViewGroup.LayoutParams? = null
) : LinearLayout(context) {
    companion object {
        const val UNKNOWN = -1
        const val UNCHECKED = 0
        const val CHECKED = 1
    }

    private val checkbox: LabelledCheckbox
    val checkboxLayout: CheckboxLinearLayout

    var checkedChangeHandler: ((Int) -> Unit)? = null
    private lateinit var checkedChildren: MutableList<View>
    private var state = UNCHECKED
        set(value) {
            if (field != value) {
                field = value
                updateCheckbox(false, notify = false)
            }
        }

    init {
        layoutParams = specialLayoutParams
            ?: LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )

        orientation = VERTICAL

        checkbox = LabelledCheckbox(context, labelText)
        checkboxLayout = CheckboxLinearLayout(context, this)

        addView(checkbox)
        addView(checkboxLayout)

        checkbox.checkedChangeHandler = {
            state = if (it) CHECKED else UNCHECKED
            updateCheckbox(false)
        }

        updateCheckbox()
    }

    fun onViewToLayoutAdded(child: View?) {
        if (child is ParentCheckbox) {
            child.checkedChangeHandler = { childState ->
                if (childState == CHECKED) {
                    if (checkedChildren.indexOf(child) == -1) {
                        checkedChildren.add(child)
                    }
                } else {
                    checkedChildren.remove(child)
                }

                updateStateByChildren()
            }
        } else if (child is LabelledCheckbox) {
            child.isChecked = true

            child.checkedChangeHandler = { isChecked ->
                if (isChecked) {
                    if (checkedChildren.indexOf(child) == -1) {
                        checkedChildren.add(child)
                    }
                } else {
                    checkedChildren.remove(child)
                }

                updateStateByChildren()
            }
        }

        updateButton()
    }

    private fun updateStateByChildren() {
        state = when(checkedChildren.size) {
            0 -> UNCHECKED
            checkboxLayout.childCount -> CHECKED
            else -> UNKNOWN
        }

        updateCheckbox(false)
    }

    private fun updateCheckbox(updateState: Boolean = true, notify: Boolean = true) {
        if (updateState) {
            state = if (state == CHECKED) UNCHECKED else CHECKED
        }

        checkbox.isChecked = state == CHECKED

        if (notify) {
            checkedChangeHandler?.invoke(state)
        }

        updateButton()
    }

    private fun updateButton() {
        if (state == CHECKED) {
            checkedChildren = mutableListOf()

            checkboxLayout.children.forEach {
                if (it is LabelledCheckbox) {
                    checkedChildren.add(it)
                    it.isChecked = true
                } else if (it is ParentCheckbox) {
                    checkedChildren.add(it)
                    it.state = CHECKED
                }
            }
        } else if (state == UNCHECKED) {
            checkedChildren = mutableListOf()

            checkboxLayout.children.forEach {
                if (it is LabelledCheckbox) {
                    it.isChecked = false
                } else if (it is ParentCheckbox) {
                    it.state = UNCHECKED
                }
            }
        }
    }
}
