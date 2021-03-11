package ru.labore.eventeger.ui.base

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
import ru.labore.eventeger.R
import ru.labore.eventeger.data.db.entities.CommentEntity
import ru.labore.eventeger.utils.hideKeyboard
import ru.labore.eventeger.utils.showKeyboard
import kotlin.properties.Delegates

abstract class DetailedAuthoredEntityViewModel(
    app: Application
) : BaseRecyclerViewModel(app) {
    lateinit var fragment: DetailedAuthoredEntityFragment
    var announcementId by Delegates.notNull<Int>()
    var replyTo: Int? = null
    private var isHidden = false
    private var currentText = ""
        set(value) {
            field = value

            (adapter as DetailedAuthoredEntityRecyclerViewAdapter).setCommentText(value)
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
                val id = if (fragment.item is CommentEntity)
                    (fragment.item as CommentEntity).announcementId
                else
                    fragment.item.id

                val comment = appRepository.createComment(
                    id,
                    currentText,
                    isHidden,
                    replyTo
                )

                comment.createdAt = appRepository.now().minusSeconds(1)

                currentText = ""

                fragment.item.commentsCount += 1

                appRepository.persistFetchedAuthoredEntity(fragment.item)

                fragment.requireActivity().runOnUiThread {
                    (adapter as DetailedAuthoredEntityRecyclerViewAdapter)
                        .onSuccessfulCreation(comment)
                }
            }, {
                fragment.requireActivity().runOnUiThread {
                    (adapter as DetailedAuthoredEntityRecyclerViewAdapter)
                        .onUnsuccessfulCreation(currentText)
                }
            })
        }
    }
}