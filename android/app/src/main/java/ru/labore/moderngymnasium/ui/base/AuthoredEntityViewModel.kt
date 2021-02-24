package ru.labore.moderngymnasium.ui.base

import android.app.Application
import android.graphics.Color
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.ui.adapters.AuthoredEntityRecyclerViewAdapter
import ru.labore.moderngymnasium.utils.hideKeyboard
import ru.labore.moderngymnasium.utils.showKeyboard

abstract class AuthoredEntityViewModel(
    app: Application
) : BaseRecyclerViewModel(app) {
    lateinit var fragment: AuthoredEntityFragment
    protected var isHidden = false
    protected var currentText = ""
        set(value) {
            field = value

            (adapter as AuthoredEntityRecyclerViewAdapter).setCommentText(value)
        }

    fun promptCommentVisibility(anchor: View) {
        val popup = PopupMenu(fragment.requireContext(), anchor)

        popup.menuInflater.inflate(R.menu.create_comment_menu, popup.menu)
        popup.menu.getItem(0).isChecked = isHidden

        popup.setOnMenuItemClickListener {
            isHidden = !it.isChecked
            it.isChecked = isHidden

            it.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
            it.actionView = View(fragment.requireContext())
            it.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionCollapse(item: MenuItem?) = false
                override fun onMenuItemActionExpand(item: MenuItem?) = false
            })

            false
        }

        popup.show()
    }

    fun enterCommentText() {
        val layout = fragment.requireView() as ViewGroup
        val context = fragment.requireContext()
        val grayScreenLayoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            0
        )

        grayScreenLayoutParams.topToTop = layout.id
        grayScreenLayoutParams.startToStart = layout.id
        grayScreenLayoutParams.bottomToBottom = layout.id
        grayScreenLayoutParams.endToEnd = layout.id
        grayScreenLayoutParams.verticalWeight = 1F

        val grayScreen = View(context)
        grayScreen.setBackgroundColor(Color.argb(128, 0, 0, 0))
        grayScreen.layoutParams = grayScreenLayoutParams

        val childLayout = LayoutInflater.from(context)
            .inflate(
                R.layout.create_comment_text_layout,
                layout,
                false
            ) as LinearLayout
        val iterator = childLayout.children.iterator()

        val editText = iterator.next() as EditText
        val button = iterator.next()

        editText.setText(currentText)
        editText.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT

        layout.addView(grayScreen)
        layout.addView(childLayout)

        editText.requestFocus()
        fragment.showKeyboard()

        grayScreen.setOnClickListener {
            val size = layout.childCount

            layout.removeViewAt(size - 1)
            layout.removeViewAt(size - 2)

            fragment.hideKeyboard()
        }

        button.setOnClickListener {
            val size = layout.childCount

            layout.removeViewAt(size - 1)
            layout.removeViewAt(size - 2)

            fragment.hideKeyboard()

            currentText = editText.text.toString()
        }
    }

    fun sendComment() {
        GlobalScope.launch {
            makeRequest(fragment.requireActivity(), {
                val comment = appRepository.createComment(
                    fragment.item.id,
                    currentText,
                    isHidden
                )

                comment.createdAt = appRepository.now().minusSeconds(1)

                currentText = ""

                fragment.item.commentCount += 1

                appRepository.persistFetchedAuthoredEntity(fragment.item)

                fragment.requireActivity().runOnUiThread {
                    (adapter as AuthoredEntityRecyclerViewAdapter)
                        .onSuccessfulCreation(comment)
                }
            }, {
                fragment.requireActivity().runOnUiThread {
                    (adapter as AuthoredEntityRecyclerViewAdapter)
                        .onUnsuccessfulCreation(currentText)
                }
            })
        }
    }
}