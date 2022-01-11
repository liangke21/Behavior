package com.liangke.viewpoint.behavior

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.customview.view.AbsSavedState
import com.liangke.viewpoint.R

class GlobalBehavior<V : View> : CoordinatorLayout.Behavior<V> {


    constructor() : super()

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        val a = context?.obtainStyledAttributes(attrs, R.styleable.GlobalBehavior)
        val direction = a?.getInt(R.styleable.GlobalBehavior_direction, -1)
        Log.d(TAG, direction.toString())
        a?.recycle()
    }


    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.fitsSystemWindows = true
        }

        if (ViewCompat.getImportantForAccessibility(child)
            == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        ) {
            ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
        }

        parent.onLayoutChild(child, layoutDirection)

        ViewCompat.offsetLeftAndRight(child, 1030)
        return true
    }


    companion object {
        private const val TAG = "GlobalBehavior"

        /**
         * 获取与view关联的GlobalBehavior
         * @param view View 带有[GlobalBehavior]的[View]
         * @return 与view关联的[GlobalBehavior]
         */
        fun <V : View?> since(view: V): GlobalBehavior<View> {
            val params = view?.layoutParams
            require(params is CoordinatorLayout.LayoutParams) { "该视图不是 CoordinatorLayout的子视图" }

            val behavior = params.behavior
            require(behavior is CoordinatorLayout.Behavior<*>) { "该视图与 GlobalBehavior无关" }
            @Direction val a = TOP_SHEET
            Log.d(TAG, " a $a  b $BOTTOM_SHEET")
            return behavior as GlobalBehavior
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @IntDef(
            TOP_SHEET,
            BOTTOM_SHEET,
            LEFT_SHEET,
            RIGHT_SHEET
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class Direction

        /**
         * 顶部工作表
         */
        const val TOP_SHEET = 1

        /**
         * 底部工作表
         */
        const val BOTTOM_SHEET = 2

        /**
         * 左部工作表
         */
        const val LEFT_SHEET = 3

        /**
         * 右部工作表
         */
        const val RIGHT_SHEET = 4


    }

}