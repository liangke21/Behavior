package com.example.myBehavior.view

import android.content.Context
import android.graphics.Rect
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.NonNull
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MyBehavior<V : View> : CoordinatorLayout.Behavior<V>  {
    constructor() : super()
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)


    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        return super.onInterceptTouchEvent(parent, child, ev)
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        return super.onTouchEvent(parent, child, ev)
    }

    override fun getScrimColor(parent: CoordinatorLayout, child: V): Int {
        return super.getScrimColor(parent, child)
    }

    override fun getScrimOpacity(parent: CoordinatorLayout, child: V): Float {
        return super.getScrimOpacity(parent, child)
    }

    override fun blocksInteractionBelow(parent: CoordinatorLayout, child: V): Boolean {
        return super.blocksInteractionBelow(parent, child)
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        return super.layoutDependsOn(parent, child, dependency)
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        return super.onDependentViewChanged(parent, child, dependency)
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: V, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)
    }

    override fun onMeasureChild(parent: CoordinatorLayout, child: V, parentWidthMeasureSpec: Int, widthUsed: Int, parentHeightMeasureSpec: Int, heightUsed: Int): Boolean {
        return super.onMeasureChild(parent, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed)
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
        ViewCompat.offsetLeftAndRight(child,1030)
        return true
    }


    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
    }

    override fun onNestedScrollAccepted(coordinatorLayout: CoordinatorLayout, child: V, directTargetChild: View, target: View, axes: Int, type: Int) {
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes, type)
    }


    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, type: Int) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }

    override fun onNestedFling(coordinatorLayout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed)
    }

    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float): Boolean {
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
    }

    override fun onApplyWindowInsets(coordinatorLayout: CoordinatorLayout, child: V, insets: WindowInsetsCompat): WindowInsetsCompat {
        return super.onApplyWindowInsets(coordinatorLayout, child, insets)
    }

    override fun onRequestChildRectangleOnScreen(coordinatorLayout: CoordinatorLayout, child: V, rectangle: Rect, immediate: Boolean): Boolean {
        return super.onRequestChildRectangleOnScreen(coordinatorLayout, child, rectangle, immediate)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: V, state: Parcelable) {
        super.onRestoreInstanceState(parent, child, state)
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: V): Parcelable? {
        return super.onSaveInstanceState(parent, child)
    }

    override fun getInsetDodgeRect(parent: CoordinatorLayout, child: V, rect: Rect): Boolean {
        return super.getInsetDodgeRect(parent, child, rect)
    }



    companion object{
        /**
         * A utility function to get the [LeftSheetBehavior] associated with the `view`.
         *
         * @param view The [View] with [LeftSheetBehavior].
         * @return The [LeftSheetBehavior] associated with the `view`.
         */
        @NonNull
        @JvmStatic
        fun <V : View?> from(view: V): MyBehavior<View> {
            val params = view?.layoutParams
            require(params is CoordinatorLayout.LayoutParams) { "该视图不是 CoordinatorLayout 的子视图" }

            val behavior = params.behavior
            require(behavior is MyBehavior<*>) { "The view is not associated with BottomSheetBehavior" }
            return behavior as MyBehavior
        }
    }
}