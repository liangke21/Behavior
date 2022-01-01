package com.example.mybottomsheetstate.view

import android.os.Parcel
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IntDef
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.customview.view.AbsSavedState

open class LeftSheetBehavior<V : View> : CoordinatorLayout.Behavior<V>() {
//<editor-fold desc="SavedState 交互变量" >
    /**
     * 状态
     */
    @State
    var state = STATE_COLLAPSED

    /**
     * 设置窥视高度
     */
    private var peekHeight = 0

    /**
     *  合适类容
     */
    private var fitToContents = true

    /**
     * 可隐藏
     */
    var hideable: Boolean = false

    /**
     * 跳过折叠
     */
    private val skipCollapsed = false
//</editor-fold>

    //<editor-fold desc="行为" >
    /**
     *保存实例状态
     */
    override fun onSaveInstanceState(parent: CoordinatorLayout, child: V): SavedState? {

        return super.onSaveInstanceState(parent, child)?.let { SavedState(it, this) }
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: V, state: Parcelable) {
        val ss=state as SavedState
        ss.superState?.let { super.onRestoreInstanceState(parent, child, it) }

    }

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        return super.onInterceptTouchEvent(parent, child, ev)
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        return super.onTouchEvent(parent, child, ev)
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, type: Int) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }

    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float): Boolean {
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
    }
//</editor-fold>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(
        STATE_EXPANDED,
        STATE_COLLAPSED,
        STATE_DRAGGING,
        STATE_SETTLING,
        STATE_HIDDEN,
        STATE_HALF_EXPANDED
    )
    /**
    @Retention(
    RetentionPolicy.SOURCE
    )
     */
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class State


    companion object {


        /**
         * 状态跨实例持久化
         */
        class SavedState : AbsSavedState {
            @State
            var state = 0 //状态
            private var peekHeight = 0 //窥视高度
            private var fitToContents = false //适合类容
            private var hideable = false //可隐藏
            private var skipCollapsed = false //跳过折叠

            @Deprecated("不可以用")
            constructor(superState: Parcelable, state: Int) : super(superState)

            constructor(@NonNull source: Parcel) : super(source, null)
            constructor(@NonNull source: Parcel, loader: ClassLoader?) : super(source, loader) {
                //不检查资源类型
                state = source.readInt()
                peekHeight = source.readInt()
                fitToContents = source.readInt() == 1
                hideable = source.readInt() == 1
                skipCollapsed = source.readInt() == 1
            }

            constructor(superState: Parcelable, @NonNull behavior: LeftSheetBehavior<*>) : super(superState) {

                this.state = behavior.state
                this.peekHeight = behavior.peekHeight
                this.fitToContents = behavior.fitToContents
                this.hideable = behavior.hideable
                this.skipCollapsed = behavior.skipCollapsed
            }

            override fun writeToParcel(out: Parcel, flags: Int) {
                super.writeToParcel(out, flags)
                out.writeInt(state)
                out.writeInt(peekHeight)
                out.writeInt(if (fitToContents) 1 else 0)
                out.writeInt(if (hideable) 1 else 0)
                out.writeInt(if (skipCollapsed) 1 else 0)
            }


            companion object {

                @JvmField
                val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.ClassLoaderCreator<SavedState> {
                    @NonNull
                    override fun createFromParcel(@NonNull source: Parcel, loader: ClassLoader): SavedState {
                        return SavedState(source, loader)
                    }

                    @NonNull
                    override fun createFromParcel(@NonNull source: Parcel): SavedState {
                        return SavedState(source, null)
                    }

                    @NonNull
                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)

                    }
                }
            }

        }


        /** 左侧工作表正在拖动.  */
        const val STATE_DRAGGING = 1

        /** The Left sheet is settling.  */
        const val STATE_SETTLING = 2

        /** The Left sheet is expanded.  */
        const val STATE_EXPANDED = 3

        /** 左侧工作表已折叠  */
        const val STATE_COLLAPSED = 4

        /** The Left sheet is hidden.  */
        const val STATE_HIDDEN = 5

        /** The Left sheet is half-expanded (used when mFitToContents is false).  */
        const val STATE_HALF_EXPANDED = 6
    }

}
