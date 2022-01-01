package com.example.myBehavior.view

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcel
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.IntDef
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.customview.view.AbsSavedState
import androidx.customview.widget.ViewDragHelper
import com.example.myBehavior.internal.ViewUtils
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.ref.WeakReference

open class LeftSheetBehavior<V : View> : CoordinatorLayout.Behavior<V>() {
//<editor-fold desc="变量" >

    //<editor-fold desc="状态" >
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
    private var skipCollapsed = false

    /**
     * 折叠偏移
     */
    var collapsedOffset = 0

    /**
     * 合适类容偏移量
     */
    var fitToContentsOffset: Int = 0
    //</editor-fold>

    //<editor-fold desc="标志" >
    @SaveFlags
    private val saveFlags = SAVE_NONE

    //</editor-fold>

    var viewRef: WeakReference<V>? = null

    var viewDragHelper: ViewDragHelper? = null


    /** 允许的最小窥视高度  */
    private var peekHeightMin = 0


    private var gestureInsetLeftIgnored = false

    /** 是否使用自动查看高度。  */
    private val peekHeightAuto = false

    /**
     * 手势插入左边
     */
    private var gestureInsetLeft = 0

    /**
     * 父宽高
     */
    var parentWidth = 0
    var parentHeight = 0

    /**
     * 子宽高
     */
   private var childHeight = 0


    /** 窥视高度手势插入缓冲区以确保足够的可滑动空间。  */
    private val peekHeightGestureInsetBuffer = 0

    /**
     * 半展开偏移
     */
    var halfExpandedOffset = 0

    var expandedOffsetL = 0
//</editor-fold>

    //<editor-fold desc="行为" >
    /**
     *保存实例状态
     */
    override fun onSaveInstanceState(parent: CoordinatorLayout, child: V): SavedState? {

        return super.onSaveInstanceState(parent, child)?.let { SavedState(it, this) }
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: V, state: Parcelable) {
        val ss = state as SavedState
        ss.superState?.let { super.onRestoreInstanceState(parent, child, it) }
        //恢复由 saveFlags 指定的可选状态值
        restoreOptionalState(ss)
        // 中间状态恢复为折叠状态
        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            this.state = STATE_COLLAPSED
        } else {
            this.state = ss.state
        }
    }

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        //这些可能已经为空，但只是为了安全，明确分配它们。这让我们知道
        // 我们第一次通过检查（viewRef == null）以这种行为进行布局。

        viewRef = null
        viewDragHelper = null
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
        // 释放引用，这样我们就不会在未附加到视图时运行不必要的代码路径。
        viewRef = null
        viewDragHelper = null
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {

        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.fitsSystemWindows = true
        }

        if (viewRef == null) {
            // 具有此行为的第一个布局。
            peekHeightMin = parent.resources.getDimensionPixelSize(R.dimen.design_bottom_sheet_peek_height_min)
            setSystemGestureInsets(child)
        }






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

    //<editor-fold desc="注解" >
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


    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(
        flag = true,
        value = [SAVE_PEEK_HEIGHT, SAVE_FIT_TO_CONTENTS, SAVE_HIDEABLE, SAVE_SKIP_COLLAPSED, SAVE_ALL, SAVE_NONE]
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class SaveFlags

    //</editor-fold>
    companion object {


        /**
         * 状态跨实例持久化
         */
        class SavedState : AbsSavedState {
            @State
            var state = 0 //状态
            var peekHeight = 0 //窥视高度
            var fitToContents = false //适合类容
            var hideable = false //可隐藏
            var skipCollapsed = false //跳过折叠

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

        //<editor-fold desc="字段" >

        //<editor-fold desc="状态" >
        /** 左侧工作表正在拖动.  */
        const val STATE_DRAGGING = 1

        /** 左表正在解决。  */
        const val STATE_SETTLING = 2

        /** 左侧工作表已展开。  */
        const val STATE_EXPANDED = 3

        /** 左侧工作表已折叠  */
        const val STATE_COLLAPSED = 4

        /** 左侧工作表被隐藏。  */
        const val STATE_HIDDEN = 5

        /** 左表是半展开的（当 mFitToContents 为 false 时使用）。  */
        const val STATE_HALF_EXPANDED = 6
        //</editor-fold>

        //<editor-fold desc="标志" >
        /**
         * 查看其父级的 16:9 比例键线。
         * 这可以用作.setPeekHeight的参数。 .getPeekHeight将在设置值时返回此值
         */
        const val PEEK_HEIGHT_AUTO = -1

        /** 此标志将在配置更改时保留 peekHeight int 值。  */
        const val SAVE_PEEK_HEIGHT = 0x1

        /** 此标志将在配置更改时保留 fitToContents 布尔值。  */
        const val SAVE_FIT_TO_CONTENTS = 1 shl 1

        /** 此标志将在配置更改时保留可隐藏的布尔值。  */
        const val SAVE_HIDEABLE = 1 shl 2

        /** 此标志将在配置更改时保留 skipCollapsed 布尔值。  */
        const val SAVE_SKIP_COLLAPSED = 1 shl 3

        /** 此标志将在配置更改时保留所有上述值。  */
        const val SAVE_ALL = -1

        /**
         * 如果视图被销毁和重新创建，此标志将不会保留在运行时设置的上述值。
         * 唯一保留的值将是位置状态，例如折叠、隐藏、展开等。这是默认行为。
         */
        const val SAVE_NONE = 0


        //</editor-fold>

        //</editor-fold>
    }
    //<editor-fold desc="私有方法区" >
    /**
     * 恢复可选状态
     */
    private fun restoreOptionalState(@NonNull ss: SavedState) {
        if (this.saveFlags == SAVE_NONE) {
            return
        }
        if (this.saveFlags == SAVE_ALL || this.saveFlags and SAVE_PEEK_HEIGHT == SAVE_PEEK_HEIGHT) {
            peekHeight = ss.peekHeight
        }
        if (this.saveFlags == SAVE_ALL
            || this.saveFlags and SAVE_FIT_TO_CONTENTS == SAVE_FIT_TO_CONTENTS
        ) {
            fitToContents = ss.fitToContents
        }
        if (this.saveFlags == SAVE_ALL || this.saveFlags and SAVE_HIDEABLE == SAVE_HIDEABLE) {
            hideable = ss.hideable
        }
        if (this.saveFlags == SAVE_ALL
            || this.saveFlags and SAVE_SKIP_COLLAPSED == SAVE_SKIP_COLLAPSED
        ) {
            skipCollapsed = ss.skipCollapsed
        }
    }

    /**
     * 确保透视高度至少与底部手势插入大小一样大，以便始终可以拖动工作表，但仅在系统需要插入时才可以拖动
     */
    private fun setSystemGestureInsets(@NonNull child: View) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q && !isGestureInsetLeftIgnored() && !peekHeightAuto) {
            ViewUtils.doOnApplyWindowInsets(child, object : ViewUtils.OnApplyWindowInsetsListener {
                override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat, initialPadding: ViewUtils.RelativePadding): WindowInsetsCompat {
                    //TODO 左换底部
                    gestureInsetLeft = insets.mandatorySystemGestureInsets.left
                    updatePeekHeight( /* animate= */false)
                    return insets
                }
            })
        }

    }

    /**
     * 更新透视高度
     */
    private fun updatePeekHeight(animate: Boolean) {
        if (viewRef != null) {
            calculateCollapsedOffset()
            if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                val view = viewRef!!.get()
                if (view != null) {
                    if (animate) {
                        settleToStatePendingLayout(state)
                    } else {
                        view.requestLayout()
                    }
                }
            }
        }
    }
    /**
     * 结算到状态待定布局
     */
    private fun settleToStatePendingLayout(@State state: Int) {
        val child = viewRef!!.get() ?: return
        // 开始动画；如果有一个待处理的布局，请等待。
        val parent = child.parent
        if (parent != null && parent.isLayoutRequested && ViewCompat.isAttachedToWindow(child)) {
            val finalState = state
            child.post { settleToState(child, finalState) }
        } else {
            settleToState(child, state)
        }
    }


    /**
     * 计算折叠偏移
     */
    private fun calculateCollapsedOffset() {
        val peek: Int = calculatePeekHeight()
        if (fitToContents) {
            collapsedOffset = Math.max(parentHeight - peek, fitToContentsOffset)
        } else {
            collapsedOffset = parentHeight - peek
        }
    }

    /**
     * 计算窥视高度
     */
    private fun calculatePeekHeight(): Int {
        if (peekHeightAuto) {
            val desiredHeight = Math.max(peekHeightMin, parentHeight - parentWidth * 9 / 16)
            return Math.min(desiredHeight, childHeight)
        }
        return if (!gestureInsetLeftIgnored && gestureInsetLeft > 0) {
            Math.max(peekHeight, gestureInsetLeft + peekHeightGestureInsetBuffer)
        } else peekHeight
    }


    //</editor-fold>

    //<editor-fold desc="公共" >


    /**
     * 返回此左工作表是否应根据系统手势区域调整其位置。
     */
    //TODO 底部换左
    open fun isGestureInsetLeftIgnored(): Boolean {
        return gestureInsetLeftIgnored
    }

    /**
     * 解决状态
     */
    open  fun settleToState(child: View, state: Int) {
       var state = state
        var top: Int
        if (state == STATE_COLLAPSED) {
            top = collapsedOffset
        } else if (state == STATE_HALF_EXPANDED) {
            top = halfExpandedOffset
            if (fitToContents && top <= fitToContentsOffset) {
                // 如果我们滚动超过内容的高度，则跳到展开状态。
                state = STATE_EXPANDED
                top = fitToContentsOffset
            }
        } else if (state == STATE_EXPANDED) {
            top = getExpandedOffset()
        } else if (hideable && state == STATE_HIDDEN) {
            top = parentHeight
        } else {
            throw IllegalArgumentException("非法状态论证: $state")
        }
        startSettlingAnimation(child, state, top, false)
    }

    /**
     * 开始稳定定动画
     */
     fun startSettlingAnimation(child: View, state: Int, top: Int, settleFromViewDragHelper: Boolean) {
        val startedSettling = (viewDragHelper != null
                //TODO 有方向未更改
                && if (settleFromViewDragHelper) viewDragHelper!!.settleCapturedViewAt(child.left, top) else viewDragHelper!!.smoothSlideViewTo(child, child.left, top))
        if (startedSettling) {
            setStateInternal(STATE_SETTLING)
            // STATE_SETTLING won't animate the material shape, so do that here with the target state.
            //TODO 未检查1
            updateDrawableForTargetState(state)
            if (settleRunnable == null) {
                // If the singleton SettleRunnable instance has not been instantiated, create it.
                settleRunnable = BottomSheetBehavior.SettleRunnable(child, state)
            }
            // If the SettleRunnable has not been posted, post it with the correct state.
            if (settleRunnable.isPosted == false) {
                settleRunnable.targetState = state
                ViewCompat.postOnAnimation(child, settleRunnable)
                settleRunnable.isPosted = true
            } else {
                // Otherwise, if it has been posted, just update the target state.
                settleRunnable.targetState = state
            }
        } else {
            setStateInternal(state)
        }
    }
    open fun setStateInternal(@State state: Int) {
        if (this.state == state) {
            return
        }
        this.state = state
        if (viewRef == null) {
            return
        }
        val bottomSheet = viewRef!!.get() ?: return
        if (state == STATE_EXPANDED) {
            //TODO 未检查2
            updateImportantForAccessibility(true)
        } else if (state == BottomSheetBehavior.STATE_HALF_EXPANDED || state == BottomSheetBehavior.STATE_HIDDEN || state == BottomSheetBehavior.STATE_COLLAPSED) {
            updateImportantForAccessibility(false)
        }
        updateDrawableForTargetState(state)
        for (i in callbacks.indices) {
            callbacks.get(i).onStateChanged(bottomSheet, state)
        }
        updateAccessibilityActions()
    }
    /**
     * 返回当前扩展的偏移量。 如果fitToContents为 true，它将根据内容的高度自动选择偏移量
     */
    open  fun getExpandedOffset(): Int {
        return if (fitToContents) fitToContentsOffset else expandedOffsetL
    }

    //</editor-fold>
}
