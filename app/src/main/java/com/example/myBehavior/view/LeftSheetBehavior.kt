package com.example.myBehavior.view


import android.animation.ValueAnimator
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcel
import android.os.Parcelable
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.core.view.accessibility.AccessibilityViewCommand.CommandArguments
import androidx.customview.view.AbsSavedState
import androidx.customview.widget.ViewDragHelper
import com.example.myBehavior.R
import com.example.myBehavior.internal.ViewUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.shape.MaterialShapeDrawable
import java.lang.ref.WeakReference

open class LeftSheetBehavior<V : View> : CoordinatorLayout.Behavior<V>() {


    /** 用于监视有关左部工作表的事件的回调。  */
    abstract class LeftSheetCallback {
        /**
         * 当左部工作表更改其状态时调用。
         *
         * @param LeftSheet 左部工作表视图。
         * @param newState 新状态。这将是其中之一 [.STATE_DRAGGING], [     ][.STATE_SETTLING], [.STATE_EXPANDED], [.STATE_COLLAPSED], [     ][.STATE_HIDDEN], or [.STATE_HALF_EXPANDED].
         */
        abstract fun onStateChanged(LeftSheet: View, @State newState: Int)

        /**
         * 在拖动左部工作表时调用。
         *
         * @param LeftSheet 左部工作表视图。
         * @param slideOffset 此左部工作表在 [-1,1] 范围内的新偏移量。偏移量增加
         * 因为这个底片正在向上移动。从 0 到 1，工作表介于折叠和
         * 展开状态，从 -1 到 0，它介于隐藏状态和折叠状态之间。
         */
        abstract fun onSlide(LeftSheet: View, slideOffset: Float)
    }


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



    /**
     * 重要无障碍的地图
     */
    private var importantForAccessibilityMap: Map<View, Int>? = null

    /**
     * 更新重要的兄弟姐妹无障碍功能
     */
    private val updateImportantForAccessibilityOnSiblings = false

    /**
     * 是形状扩展
     */
    private var isShapeExpanded = false

    /**
     * 材料形状可绘制
     */
    private val materialShapeDrawable: MaterialShapeDrawable? = null

    /**
     * 插值动画师
     */
    private val interpolatorAnimator: ValueAnimator? = null

    /**
     * 回调
     */
    private val callbacks = ArrayList<LeftSheetCallback>()

    /**
     * 展开中途行动 ID
     */
    private var expandHalfwayActionId = View.NO_ID

    private var settleRunnable: SettleRunnable? = null

    /** 如果 Behavior 的 @shapeAppearance 属性具有非空值，则为 True  */
    private val shapeThemingEnabled = false

    var elevation = -1f

    var halfExpandedRatio = 0.5f

    private var ignoreEvents = false

    /**
     * 速度追踪器
     */
    private var velocityTracker: VelocityTracker? = null

    /**
     * 初始化y
     */
    private var initialY = 0
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
            setSystemGestureInsets(child)//TODO 核心代码
            viewRef = WeakReference(child)
            //如果启用 shapeTheming，则仅将 MaterialShapeDrawable 设置为背景，否则将
            //默认为 android:background 在样式或布局中声明。

            if (shapeThemingEnabled && materialShapeDrawable != null) {
                ViewCompat.setBackground(child, materialShapeDrawable)
            }

            // 在 MaterialShapeDrawable 上设置高程
            if (materialShapeDrawable != null) {
                // 如果在底页上设置，则使用高程属性；否则，使用子视图的高度。
                materialShapeDrawable.elevation = if (elevation == -1f) ViewCompat.getElevation(child) else elevation
                // 根据初始状态更新材料形状。
                isShapeExpanded = state == STATE_EXPANDED
                materialShapeDrawable.interpolation = if (isShapeExpanded) 0f else 1f
            }
            updateAccessibilityActions()
            if (ViewCompat.getImportantForAccessibility(child)
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            ) {
                ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
            }
        }
        if (viewDragHelper == null) {
            //TODO 核心代码3
            viewDragHelper = dragCallback?.let { ViewDragHelper.create(parent, it) }
        }
        //TODO 顶部换右边
        val savedTop = child.right
        // 首先让父级布局
        parent.onLayoutChild(child, layoutDirection)
        // 偏移底部纸张
        parentWidth = parent.width
        parentHeight = parent.height
        childHeight = child.height
        fitToContentsOffset = Math.max(0, parentHeight - childHeight)

        calculateHalfExpandedOffset()
        calculateCollapsedOffset()
        // TODO 顶部和底部换左部和右部
        if (state == STATE_EXPANDED) {
            ViewCompat.offsetLeftAndRight(child, getExpandedOffset())
        } else if (state == STATE_HALF_EXPANDED) {
            ViewCompat.offsetLeftAndRight(child, halfExpandedOffset)
        } else if (hideable && state == STATE_HIDDEN) {
            ViewCompat.offsetLeftAndRight(child, parentHeight)
        } else if (state == STATE_COLLAPSED) {
            ViewCompat.offsetLeftAndRight(child, collapsedOffset)
        } else if (state == STATE_DRAGGING || state == STATE_SETTLING) {
            //TODO 顶部换右边
            ViewCompat.offsetLeftAndRight(child, savedTop - child.right)
        }

        nestedScrollingChildRef = WeakReference<View>(findScrollingChild(child))
        return true
    }

    /**
     * 拦截触摸事件
     */
    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        if (!child.isShown || !draggable) {
            ignoreEvents = true
            return false
        }
        val action: Int = ev.getActionMasked()
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) { //按下
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)
        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchingScrollingChild = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                // Reset the ignore flag
                if (ignoreEvents) {
                    ignoreEvents = false
                    return false
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val initialX =ev.getX() as Int
                initialY = ev.getY() as Int
                // Only intercept nested scrolling events here if the view not being moved by the
                // ViewDragHelper.
                if (state != STATE_SETTLING) {
                    val scroll = if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                        activePointerId = ev.getPointerId(ev.getActionIndex())
                        touchingScrollingChild = true
                    }
                }
                ignoreEvents = (activePointerId == MotionEvent.INVALID_POINTER_ID
                        && !parent.isPointInChildBounds(child, initialX, initialY))
            }
            else -> {}
        }
        if (!ignoreEvents
            && viewDragHelper != null && viewDragHelper!!.shouldInterceptTouchEvent(ev)
        ) {
            return true
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.

        val scroll = if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null
        return (action == MotionEvent.ACTION_MOVE && scroll != null && !ignoreEvents
                && state != STATE_DRAGGING && !parent.isPointInChildBounds(scroll, ev.getX() as Int, ev.getY() as Int)
                && viewDragHelper != null && Math.abs(initialY - ev.getY()) > viewDragHelper!!.touchSlop)
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        if (!child.isShown) {
            return false
        }
        val action: Int = ev.getActionMasked()
        if (state == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (viewDragHelper != null) {
            viewDragHelper!!.processTouchEvent(ev)
        }
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(ev)
        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the bottom sheet in case it is not captured and the touch slop is passed.
        if (viewDragHelper != null && action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            if (Math.abs(initialY - ev.getY()) > viewDragHelper!!.touchSlop) {
                viewDragHelper!!.captureChildView(child, ev.getPointerId(ev.getActionIndex()))
            }
        }
        return !ignoreEvents
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


        private const val SIGNIFICANT_VEL_THRESHOLD = 500

        private const val HIDE_THRESHOLD = 0.5f

        private const val HIDE_FRICTION = 0.1f

        private const val CORNER_ANIMATION_DURATION = 500

        private const val fitToContents = true

        private const val updateImportantForAccessibilityOnSiblings = false

        private const val maximumVelocity = 0f


        //</editor-fold>

        //</editor-fold>
    }

    //<editor-fold desc="匿名内" >

    /**
     * 触摸滚动的孩子
     */
    var touchingScrollingChild: Boolean = false

    /**
     * 活动指针 ID
     */
    var activePointerId = 0

    /**
     * 嵌套滚动子引用
     */
    var nestedScrollingChildRef: WeakReference<View>? = null

    /**
     * 可拖动的
     */
    private val draggable = true

    /**
     *   扩展偏移
     */
    var expandedOffsetL =0


    /**
     * 拖动回调
     */
    private val dragCallback: ViewDragHelper.Callback? = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (state == STATE_DRAGGING) {
                return false
            }
            if (touchingScrollingChild) {
                return false
            }
            if (state == STATE_EXPANDED && activePointerId == pointerId) {
                val scroll: View? = nestedScrollingChildRef?.get()
                if (scroll != null && scroll.canScrollVertically(-1)) {
                    // 让内容向上滚动
                    return false
                }
            }
            return viewRef != null && viewRef!!.get() === child
        }

        override fun onViewPositionChanged(
            changedView: View, left: Int, top: Int, dx: Int, dy: Int
        ) {
            // TODO 顶部换左边
            dispatchOnSlide(left)
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING && draggable) {
                setStateInternal(STATE_DRAGGING)
            }
        }

        private fun releasedLow(child: View): Boolean {
            // 需要至少到左部的一半。
            return child.top > (parentHeight + getExpandedOffset()) / 2
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val top: Int
            @State val targetState: Int
            if (yvel < 0) { // Moving up
                if (fitToContents) {
                    top = fitToContentsOffset
                    targetState =STATE_EXPANDED
                } else {//TODO 顶部换右边
                    val currentTop = releasedChild.right
                    if (currentTop > halfExpandedOffset) {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    } else {
                        top = expandedOffsetL
                        targetState = STATE_EXPANDED
                    }
                }
            } else if (hideable && shouldHide(releasedChild, yvel)) {
                // Hide if the view was either released low or it was a significant vertical swipe
                // otherwise settle to closest expanded state.
                if (Math.abs(xvel) < Math.abs(yvel) && yvel > SIGNIFICANT_VEL_THRESHOLD
                    || releasedLow(releasedChild)
                ) {
                    top = parentHeight
                    targetState = STATE_HIDDEN
                } else if (fitToContents) {
                    top = fitToContentsOffset
                    targetState = STATE_EXPANDED
                    //TODO 顶部换右边
                } else if (Math.abs(releasedChild.right - expandedOffsetL)
                    //TODO 顶部换右边
                    < Math.abs(releasedChild.right - halfExpandedOffset)
                ) {
                    top = expandedOffsetL
                    targetState = STATE_EXPANDED
                } else {
                    top = halfExpandedOffset
                    targetState = STATE_HALF_EXPANDED
                }
            } else if (yvel == 0f || Math.abs(xvel) > Math.abs(yvel)) {
                // If the Y velocity is 0 or the swipe was mostly horizontal indicated by the X velocity
                // being greater than the Y velocity, settle to the nearest correct height.
                //TODO 顶部换右边
                val currentTop = releasedChild.right
                if (fitToContents) {
                    if (Math.abs(currentTop - fitToContentsOffset)
                        < Math.abs(currentTop - collapsedOffset)
                    ) {
                        top = fitToContentsOffset
                        targetState = STATE_EXPANDED
                    } else {
                        top = collapsedOffset
                        targetState = STATE_COLLAPSED
                    }
                } else {
                    if (currentTop < halfExpandedOffset) {
                        if (currentTop < Math.abs(currentTop - collapsedOffset)) {
                            top = expandedOffsetL
                            targetState = STATE_EXPANDED
                        } else {
                            top = halfExpandedOffset
                            targetState = STATE_HALF_EXPANDED
                        }
                    } else {
                        if (Math.abs(currentTop - halfExpandedOffset)
                            < Math.abs(currentTop - collapsedOffset)
                        ) {
                            top = halfExpandedOffset
                            targetState = STATE_HALF_EXPANDED
                        } else {
                            top = collapsedOffset
                            targetState =STATE_COLLAPSED
                        }
                    }
                }
            } else { // Moving Down
                if (fitToContents) {
                    top = collapsedOffset
                    targetState = STATE_COLLAPSED
                } else {
                    // Settle to the nearest correct height.
                    //TODO 顶部换右边
                    val currentTop = releasedChild.right
                    if (Math.abs(currentTop - halfExpandedOffset)
                        < Math.abs(currentTop - collapsedOffset)
                    ) {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    } else {
                        top = collapsedOffset
                        targetState = STATE_COLLAPSED
                    }
                }
            }
            startSettlingAnimation(releasedChild, targetState, top, true)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return MathUtils.clamp(
                top, getExpandedOffset(), if (hideable) parentHeight else collapsedOffset
            )
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return child.left
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return if (hideable) {
                parentHeight
            } else {
                collapsedOffset
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="内部内" >

    private inner class SettleRunnable(private val view: View, @State var targetState: Int) : Runnable {

        var isPosted = false
        override fun run() {
            if (viewDragHelper != null && viewDragHelper!!.continueSettling(true)) {
                ViewCompat.postOnAnimation(view, this)
            } else {
                setStateInternal(targetState)
            }
            isPosted = false
        }
    }


    //</editor-fold>


    //<editor-fold desc="私有方法区" >



    private  fun reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
    }


    //<editor-fold desc="计算半扩展偏移" >
    private fun calculateHalfExpandedOffset() {
        halfExpandedOffset = (parentHeight * (1 - halfExpandedRatio)) as Int
    }
    //</editor-fold>

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
            if (state == STATE_COLLAPSED) {
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

    //<editor-fold desc="更新重要的辅助功能" >
    private fun updateImportantForAccessibility(expanded: Boolean) {
        if (viewRef == null) {
            return
        }
        val viewParent = viewRef!!.get()!!.parent as? CoordinatorLayout ?: return
        val childCount = viewParent.childCount
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN && expanded) {
            if (importantForAccessibilityMap == null) {
                importantForAccessibilityMap = HashMap<View, Int>(childCount)
            } else {
                // 已经保存了子视图的可访问性值的重要。
                return
            }
        }
        for (i in 0 until childCount) {
            val child = viewParent.getChildAt(i)
            if (child === viewRef!!.get()) {
                continue
            }
            if (expanded) {
                // 保存子视图的可访问性值的重要。
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                    (importantForAccessibilityMap as HashMap<View, Int>).put(child, child.importantForAccessibility)


                }
                if (updateImportantForAccessibilityOnSiblings) {
                    ViewCompat.setImportantForAccessibility(
                        child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    )
                }
            } else {
                if (updateImportantForAccessibilityOnSiblings
                    && importantForAccessibilityMap != null && importantForAccessibilityMap!!.containsKey(child)
                ) {
                    // 恢复子视图的可访问性值的原始重要。
                    importantForAccessibilityMap!!.get(child)?.let { ViewCompat.setImportantForAccessibility(child, it) }
                }
            }
        }
        if (!expanded) {
            importantForAccessibilityMap = null
        } else if (updateImportantForAccessibilityOnSiblings) {
            // If the siblings of the bottom sheet have been set to not important for a11y, move the focus
            // to the bottom sheet when expanded.
            viewRef!!.get()!!.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }
    //</editor-fold>

    //<editor-fold desc="更新目标状态的可绘制对象" >
    private fun updateDrawableForTargetState(@State state: Int) {
        if (state == STATE_SETTLING) {
            // 特殊情况：我们想知道我们正在解决哪个状态，所以等待另一个调用。
            return
        }
        val expand = state == STATE_EXPANDED
        if (isShapeExpanded != expand) {
            isShapeExpanded = expand
            if (materialShapeDrawable != null && interpolatorAnimator != null) {
                if (interpolatorAnimator.isRunning()) {
                    interpolatorAnimator.reverse()
                } else {
                    val to = if (expand) 0f else 1f
                    val from = 1f - to
                    interpolatorAnimator.setFloatValues(from, to)
                    interpolatorAnimator.start()
                }
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="更新辅助功能操作" >
    private fun updateAccessibilityActions() {
        if (viewRef == null) {
            return
        }
        val child = viewRef!!.get() ?: return
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_COLLAPSE)
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_EXPAND)
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_DISMISS)
        if (expandHalfwayActionId != View.NO_ID) {
            ViewCompat.removeAccessibilityAction(child, expandHalfwayActionId)
        }
        if (state != STATE_HALF_EXPANDED) {
            expandHalfwayActionId = addAccessibilityActionForState(
                child, R.string.leftsheet_action_expand_halfway, STATE_HALF_EXPANDED
            )
        }
        if (hideable && state != STATE_HIDDEN) {

            replaceAccessibilityActionForState(
                child, AccessibilityActionCompat.ACTION_DISMISS, STATE_HIDDEN
            )
        }
        when (state) {
            STATE_EXPANDED -> {
                val nextState = if (fitToContents) STATE_COLLAPSED else STATE_HALF_EXPANDED
                replaceAccessibilityActionForState(
                    child, AccessibilityActionCompat.ACTION_COLLAPSE, nextState
                )
            }
            STATE_HALF_EXPANDED -> {
                replaceAccessibilityActionForState(
                    child, AccessibilityActionCompat.ACTION_COLLAPSE, STATE_COLLAPSED
                )
                replaceAccessibilityActionForState(
                    child, AccessibilityActionCompat.ACTION_EXPAND, STATE_EXPANDED
                )
            }
            STATE_COLLAPSED -> {
                val nextState = if (fitToContents) STATE_EXPANDED else STATE_HALF_EXPANDED
                replaceAccessibilityActionForState(
                    child, AccessibilityActionCompat.ACTION_EXPAND, nextState
                )
            }
            else -> {}
        }
    }
    //</editor-fold>

    //<editor-fold desc="为状态添加可访问性操作" >
    private fun addAccessibilityActionForState(child: V, @StringRes stringResId: Int, state: Int): Int {
        return ViewCompat.addAccessibilityAction(
            child,
            child.resources.getString(stringResId),
            createAccessibilityViewCommandForState(state)
        )
    }
    //</editor-fold>

    //<editor-fold desc="为状态创建可访问性视图命令" >
    private fun createAccessibilityViewCommandForState(state: Int): AccessibilityViewCommand {
        return AccessibilityViewCommand { _, _ ->
            setState(state)
            true
        }
    }
    //</editor-fold>

    //<editor-fold desc="替换状态的可访问性操作" >
    private fun replaceAccessibilityActionForState(
        child: V, action: AccessibilityActionCompat, state: Int
    ) {
        ViewCompat.replaceAccessibilityAction(
            child, action, null, createAccessibilityViewCommandForState(state)
        )
    }
    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="公共" >
    /**
     * 找到滚动的孩子
     */
    @VisibleForTesting
    open fun findScrollingChild(view: View?): View? {
        if (ViewCompat.isNestedScrollingEnabled(view!!)) {
            return view
        }
        if (view is ViewGroup) {
            val group = view
            var i = 0
            val count = group.childCount
            while (i < count) {
                val scrollingChild = findScrollingChild(group.getChildAt(i))
                if (scrollingChild != null) {
                    return scrollingChild
                }
                i++
            }
        }
        return null
    }

    /**
     * 应该隐藏
     */
     fun shouldHide(child: View, yvel: Float): Boolean {
        if (skipCollapsed) {
            return true
        }//TODO 顶部换右边
        if (child.right < collapsedOffset) {
            // 它不应该隐藏，而是崩溃。
            return false
        }
        val peek = calculatePeekHeight()
        //TODO 顶部换右边
        val newTop = child.right + yvel * HIDE_FRICTION
        return Math.abs(newTop - collapsedOffset) / peek.toFloat() > HIDE_THRESHOLD
    }


    /**
     * 在幻灯片上发送
     */
    fun dispatchOnSlide(top: Int) {
        val bottomSheet: View? = viewRef!!.get()
        if (bottomSheet != null && !callbacks.isEmpty()) {
            val slideOffset =
                if (top > collapsedOffset || collapsedOffset == getExpandedOffset()) (collapsedOffset - top).toFloat() / (parentHeight - collapsedOffset) else (collapsedOffset - top).toFloat() / (collapsedOffset - getExpandedOffset())
            for (i in callbacks.indices) {
                callbacks[i].onSlide(bottomSheet, slideOffset)
            }
        }
    }


    /**
     * 设置左部工作表的状态。左部工作表将转换为该状态
     * 动画片。
     *
     * @param state 之一 {@link #STATE_COLLAPSED}, {@link #STATE_EXPANDED}, {@link #STATE_HIDDEN},
     *     or {@link #STATE_HALF_EXPANDED}.
     */
    @JvmName("setState1")
    fun setState(@State state: Int) {
        if (state == this.state) {
            return
        }
        if (viewRef == null) {
            // 视图尚未布局；修改 mState 并让 onLayoutChild 稍后处理
            if (state == STATE_COLLAPSED || state == STATE_EXPANDED || state == STATE_HALF_EXPANDED || hideable && state == STATE_HIDDEN) {
                this.state = state
            }
            return
        }
        settleToStatePendingLayout(state)
    }


    /**
     * 返回此左工作表是否应根据系统手势区域调整其位置。
     */
    //TODO 底部换左
    fun isGestureInsetLeftIgnored(): Boolean {
        return gestureInsetLeftIgnored
    }

    /**
     * 解决状态
     */
    fun settleToState(child: View, state: Int) {
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
            // STATE_SETTLING 不会对材质形状进行动画处理，因此请在此处使用目标状态进行动画处理。

            updateDrawableForTargetState(state)//TODO 核心代码1

            if (settleRunnable == null) {
                // 如果尚未实例化单例 SettleRunnable 实例，请创建它。
                settleRunnable = SettleRunnable(child, state)
            }
            // 如果 SettleRunnable 尚未发布，请将其发布为正确的状态。
            if (!settleRunnable!!.isPosted) {
                settleRunnable!!.targetState = state
                ViewCompat.postOnAnimation(child, settleRunnable)
                settleRunnable!!.isPosted = true
            } else {
                // 否则，如果已经发布，只需更新目标状态。
                settleRunnable!!.targetState = state
            }
        } else {
            setStateInternal(state)
        }
    }

    fun setStateInternal(@State state: Int) {
        if (this.state == state) {
            return
        }
        this.state = state
        if (viewRef == null) {
            return
        }
        val leftSheet = viewRef!!.get() ?: return
        if (state == STATE_EXPANDED) {

            updateImportantForAccessibility(true)
        } else if (state == STATE_HALF_EXPANDED || state == STATE_HIDDEN || state == STATE_COLLAPSED) {
            updateImportantForAccessibility(false)
        }

        updateDrawableForTargetState(state)
        for (i in callbacks.indices) {
            callbacks.get(i).onStateChanged(leftSheet, state)
        }
        updateAccessibilityActions()
    }

    /**
     * 返回当前扩展的偏移量。 如果fitToContents为 true，它将根据内容的高度自动选择偏移量
     */
    fun getExpandedOffset(): Int {
        return if (fitToContents) fitToContentsOffset else expandedOffsetL
    }

    //</editor-fold>
}
