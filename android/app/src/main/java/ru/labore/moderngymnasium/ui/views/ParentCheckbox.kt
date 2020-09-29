package ru.labore.moderngymnasium.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.view.children
import kotlinx.android.synthetic.main.activity_login.view.*
import ru.labore.moderngymnasium.R

class ParentCheckbox : ViewGroup {
    companion object {
        private const val UNKNOWN = -1
        private const val UNCHECKED = 0
        private const val CHECKED = 1
    }

    private var checkedChangeHandler: ((Int) -> Unit)? = null
    private lateinit var checkedChildren: MutableList<View>
    private var state = 0
        set(value) {
            field = value
            updateButton()
        }

    constructor(context: Context): super(context) {
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet):
            super(context, attributeSet) {
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int):
        super(context, attributeSet, defStyleAttr) {
        init()
    }

    private fun init() {
        state = CHECKED
        updateButton()

        children.forEach {
            if (it is ViewGroup) {
                (it as ParentCheckbox).setOnCheckedChangeListener { childState ->
                    if (childState == CHECKED) {
                        if (checkedChildren.indexOf(it) == -1) {
                            checkedChildren.add(it)
                        }
                    } else {
                        checkedChildren.remove(it)
                    }
                }
            } else {
                (it as CheckBox).setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (checkedChildren.indexOf(it) == -1) {
                            checkedChildren.add(it)
                        }
                    } else {
                        checkedChildren.remove(it)
                    }
                }
            }
        }
        
        setOnClickListener {
            state = when(state) {
                CHECKED -> UNCHECKED
                else -> CHECKED
            }

            checkedChangeHandler?.let {
                it(state)
            }

            updateButton()
        }
    }

    private fun updateButton() {

        if (state == CHECKED) {
            checkedChildren = mutableListOf()

            children.forEach {
                checkedChildren.add(it)
                (it as CheckBox).isChecked = true
            }
        } else if (state == UNCHECKED) {
            checkedChildren = mutableListOf()
        }
    }

    fun setOnCheckedChangeListener(lambda: (Int) -> Unit) {
        checkedChangeHandler = lambda
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        TODO("Not yet implemented")
    }
}