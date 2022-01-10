package com.liangke.viewpoint.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat

class GlobalBehavior<V : View> : CoordinatorLayout.Behavior<V> {


    constructor() : super()
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)


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

            return behavior as GlobalBehavior
        }
    }

}