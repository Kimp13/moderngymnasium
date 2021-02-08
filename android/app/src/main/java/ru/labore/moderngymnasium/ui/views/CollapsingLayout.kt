package ru.labore.moderngymnasium.ui.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import ru.labore.moderngymnasium.R

class CollapsingLayout(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context) {
    private val toolbar: ConstraintLayout
    private var collapsed = false
    private val animator = ValueAnimator()
    private val params = layoutParams ?: LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    init {
        orientation = LinearLayout.VERTICAL

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CollapsingLayout,
            0,
            0
        ).apply {
            try {
                val label = getString(
                    R.styleable.CollapsingLayout_label
                ) ?: "CollapsingLayout"

                toolbar = LayoutInflater.from(context).inflate(
                    R.layout.collapsing_layout,
                    this@CollapsingLayout,
                    false
                ) as ConstraintLayout

                toolbar.layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                (toolbar.getChildAt(0) as TextView).text = label

                toolbar.setOnClickListener {
                    if (collapsed) {
                        expandContent() // Made in functions for convenience
                        rotateShevronBack() // AND kind of encapsulation! ^_^
                    } else {
                        collapseContent() // ditto - a new fancy word
                        rotateShevron() // ditto
                    }

                    collapsed = !collapsed
                }

                addView(toolbar, 0)

                animator.duration = 300
            } finally {
                recycle()
            }
        }
    }

    /**
     * Expand all children except the toolbar
     *
     * TODO animation
     */
    private fun expandContent() {
        this.measure(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val height = this.measuredHeight

        animator.removeAllUpdateListeners()
        animator.setIntValues(this.height, height)
        animator.addUpdateListener {
            params.height =
                if (it.currentPlayTime == it.duration)
                    ViewGroup.LayoutParams.WRAP_CONTENT
                else
                    it.animatedValue as Int

            layoutParams = params
        }

        animator.start()
    }

    /**
     * Collapse all children except the toolbar
     *
     * TODO animation
     */
    private fun collapseContent() {
        animator.removeAllUpdateListeners()
        animator.setIntValues(this.height, toolbar.height)
        animator.addUpdateListener {
            params.height = it.animatedValue as Int

            layoutParams = params
        }

        animator.start()
    }

    /**
     * Rotate shevron indicator by 180°
     *
     * TODO animation
     */
    private fun rotateShevron() {
        toolbar.getChildAt(1).rotation = 180F
    }

    /**
     * Rotate shevron back to 0°
     *
     * TODO animation
     */
    private fun rotateShevronBack() {
        toolbar.getChildAt(1).rotation = 0F
    }
}