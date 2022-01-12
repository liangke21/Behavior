package com.liangke.viewpoint.behavior

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.liangke.viewpoint.R
import com.liangke.viewpoint.enum.Direction
import com.liangke.viewpoint.enum.Direction.*

class GlobalBehavior<V : View> : CoordinatorLayout.Behavior<V> {


    var direction = BOTTOM_SHEET


    /**
     * 父宽高
     */
    private var parentWidth = 0
    private var parentHeight = 0

    /**
     * 子宽高
     */
    private var childHeight = 0
    private var childWidth = 0

    private var peekHeight = 0 //设置窥视高度

    var collapsedOffset = 0 //折叠偏移
    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.GlobalBehavior)
        val direction = a.getInt(R.styleable.GlobalBehavior_direction, 1)
        setFoldDirection(Direction.values()[direction - 1])
        val dimensionHeight = a.getDimension(R.styleable.GlobalBehavior_gb_peekHeight, -1F)
        setPeekHeight(dimensionHeight)
        a.recycle()
    }

    private fun setPeekHeight(dimensionHeight: Float) {
        peekHeight = dimensionHeight.toInt()
    }


    fun setFoldDirection(direction: Direction) {
        when (direction) {
            BOTTOM_SHEET -> {
                this.direction = BOTTOM_SHEET
            }
            TOP_SHEET -> {
                this.direction = TOP_SHEET
            }
            LEFT_SHEET -> {
                this.direction = LEFT_SHEET
            }
            RIGHT_SHEET -> {
                this.direction = RIGHT_SHEET
            }
        }
        Log.d(TAG, this.direction.name)
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

        parentWidth = parent.width
        parentHeight = parent.height

        childWidth = child.width
        childHeight = child.height

        calculateCollapsedOffset()
        handlingFoldOrientation(child)

        return true
    }

    private fun calculateCollapsedOffset() {

        collapsedOffset = when (direction) {
            BOTTOM_SHEET -> {
                Log.d(TAG,peekHeight.toString())
                childHeight-peekHeight
            }
            TOP_SHEET -> {
                -(childHeight-peekHeight)
            }
            LEFT_SHEET -> {
                -(childWidth-peekHeight)
            }
            RIGHT_SHEET -> {
                (childWidth-peekHeight)
            }
        }
    }

    /**
     * 处理折叠方向
     */
    private fun handlingFoldOrientation(child: View) {
        when (direction) {
            BOTTOM_SHEET -> {
               ViewCompat.offsetTopAndBottom(child,collapsedOffset)
            }
            TOP_SHEET -> {
            ViewCompat.offsetTopAndBottom(child,collapsedOffset)
            }
            LEFT_SHEET -> {
              ViewCompat.offsetLeftAndRight(child,collapsedOffset)
            }
            RIGHT_SHEET -> {
               ViewCompat.offsetLeftAndRight(child,collapsedOffset)
            }
        }

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

            return behavior as GlobalBehavior
        }
    }

}