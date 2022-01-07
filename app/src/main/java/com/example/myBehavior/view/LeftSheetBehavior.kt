package com.example.myBehavior.view


import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.customview.view.AbsSavedState
import androidx.customview.widget.ViewDragHelper
import com.example.myBehavior.R
import com.example.myBehavior.internal.ViewUtils
import com.example.myBehavior.koltin.LogT.lll
import com.example.myBehavior.koltin.negate

import com.google.android.material.resources.MaterialResources
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java.lang.ref.WeakReference

class LeftSheetBehavior<V : View> : CoordinatorLayout.Behavior<V> {


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


    @State
    var state = STATE_COLLAPSED  //状态

    @SaveFlags
    private var saveFlags = SAVE_NONE // 保存标志

    /**
     * 父宽高
     */
    var parentWidth = 0
    var parentHeight = 0

    /**
     * 子宽高
     */
    private var childHeight = 0
    private var childWidth = 0

    var viewRef: WeakReference<V>? = null // 弱应用 View
    var viewDragHelper: ViewDragHelper? = null //查看拖动助手
    private var peekHeight = 0 //设置窥视高度
    private var peekHeightAuto = false //是否使用自动查看高度。
    private var peekHeightMin = 0 //允许的最小窥视高度
    private var skipCollapsed = false //跳过折叠
    private var isShapeExpanded = false //是形状扩展
    var halfExpandedOffset = 0 //半展开偏移
    var halfExpandedRatio = 0.5f //半展开率
    private var expandHalfwayActionId = View.NO_ID //展开中途行动 ID
    var collapsedOffset = 0 //折叠偏移
    var expandedOffsetL = 0 //扩展偏移
    var fitToContentsOffset: Int = 0 //合适类容偏移量
    private var lastNestedScrollDy = 0 //最后一个嵌套滚动 Dy
    private var nestedScrolled = false //嵌套滚动
    var touchingScrollingChild: Boolean = false //触摸滚动的孩子
    var nestedScrollingChildRef: WeakReference<View>? = null //嵌套滚动子引用
    private var gestureInsetLeftIgnored = false //手势插入左忽略
    private var gestureInsetLeft = 0 //手势插入左边
    private var peekHeightGestureInsetBuffer = 0 //窥视高度手势插入缓冲区以确保足够的可滑动空间。
    private var draggable = true //可拖动的
    private var fitToContents = true //合适类容
    var hideable: Boolean = false //可隐藏
    private var importantForAccessibilityMap: Map<View, Int>? = null //重要无障碍的地图
    private val updateImportantForAccessibilityOnSiblings = false //更新重要的兄弟姐妹无障碍功能
    private var materialShapeDrawable: MaterialShapeDrawable? = null//材料形状可绘制
    private var interpolatorAnimator: ValueAnimator? = null//插值动画师
    private val callbacks = ArrayList<LeftSheetCallback>()//回调
    private var settleRunnable: SettleRunnable? = null //解决 Runnable
    private var shapeThemingEnabled = false//如果 Behavior 的 @shapeAppearance 属性具有非空值，则为 True
    var elevation = -1f //海拔
    private var velocityTracker: VelocityTracker? = null//速度追踪器
    private var initialY = 0 //初始化y
    private var ignoreEvents = false //忽略事假
    private var shapeAppearanceModelDefault: ShapeAppearanceModel? = null //用于底片的默认形状外观
    var activePointerId = 0 //活动指针 ID
    private val DEF_STYLE_RES = R.style.Widget_Design_BottomSheet_Modal //DEF 风格资源
    //</editor-fold>

    //<editor-fold desc="构造函数" >


    constructor() : super()

    @SuppressLint("RestrictedApi")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {

        Log.d(TAG, "执行构造函数")
        peekHeightGestureInsetBuffer = context.resources.getDimensionPixelSize(R.dimen.mtrl_min_touch_target_size)
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior_Layout)
        shapeThemingEnabled = a.hasValue(R.styleable.BottomSheetBehavior_Layout_shapeAppearance)
        val hasBackgroundTint = a.hasValue(R.styleable.BottomSheetBehavior_Layout_backgroundTint)
        if (hasBackgroundTint) {
            val bottomSheetColor = MaterialResources.getColorStateList(
                context, a, R.styleable.BottomSheetBehavior_Layout_backgroundTint
            )
            createMaterialShapeDrawable(context, attrs!!, hasBackgroundTint, bottomSheetColor)
        } else {
            createMaterialShapeDrawable(context, attrs!!, hasBackgroundTint)
        }
        createShapeValueAnimator()
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            elevation = a.getDimension(R.styleable.BottomSheetBehavior_Layout_android_elevation, -1f)
        }
        var value = a.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight)
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            setPeekHeight(value.data)
        } else {
            setPeekHeight(
                a.getDimensionPixelSize(
                    R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_HEIGHT_AUTO
                )
            )
        }
        setHideable(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false))
        setGestureInsetBottomIgnored(
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_gestureInsetBottomIgnored, false)
        )
        setFitToContents(
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_fitToContents, true)
        )
        setSkipCollapsed(
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false)
        )
        setDraggable(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_draggable, true))
        setSaveFlags(a.getInt(R.styleable.BottomSheetBehavior_Layout_behavior_saveFlags, SAVE_NONE))
        setHalfExpandedRatio(
            a.getFloat(R.styleable.BottomSheetBehavior_Layout_behavior_halfExpandedRatio, 0.5f)
        )
        value = a.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_expandedOffset)
        if (value != null && value.type == TypedValue.TYPE_FIRST_INT) {
            setExpandedOffset(value.data)
        } else {
            setExpandedOffset(
                a.getDimensionPixelOffset(
                    R.styleable.BottomSheetBehavior_Layout_behavior_expandedOffset, 0
                )
            )
        }
        a.recycle()
        val configuration = ViewConfiguration.get(context)
        maximumVelocity = configuration.scaledMaximumFlingVelocity.toFloat()
    }

    //</editor-fold>


    //<editor-fold desc="构造方法的延伸" >


    /**
     * Determines the top offset of the BottomSheet in the [.STATE_EXPANDED] state when
     * fitsToContent is false. The default value is 0, which results in the sheet matching the
     * parent's top.
     *
     * @param offset an integer value greater than equal to 0, representing the [     ][.STATE_EXPANDED] offset. Value must not exceed the offset in the half expanded state.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_expandedOffset
     */
    open fun setExpandedOffset(offset: Int) {
        require(offset >= 0) { "offset must be greater than or equal to 0" }
        this.expandedOffsetL = offset
    }


    /**
     * Determines the height of the BottomSheet in the [.STATE_HALF_EXPANDED] state. The
     * material guidelines recommended a value of 0.5, which results in the sheet filling half of the
     * parent. The height of the BottomSheet will be smaller as this ratio is decreased and taller as
     * it is increased. The default value is 0.5.
     *
     * @param ratio a float between 0 and 1, representing the [.STATE_HALF_EXPANDED] ratio.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_halfExpandedRatio
     */
    @JvmName("setHalfExpandedRatio1")
    fun setHalfExpandedRatio(@FloatRange(from = 0.0, to = 1.0) ratio: Float) {
        require(!(ratio <= 0 || ratio >= 1)) { "ratio must be a float value between 0 and 1" }
        halfExpandedRatio = ratio
        // If sheet is already laid out, recalculate the half expanded offset based on new setting.
        // Otherwise, let onLayoutChild handle this later.
        if (viewRef != null) {
            calculateHalfExpandedOffset()
        }
    }


    /**
     * Sets save flags to be preserved in bottomsheet on configuration change.
     *
     * @param flags bitwise int of [.SAVE_PEEK_HEIGHT], [.SAVE_FIT_TO_CONTENTS], [     ][.SAVE_HIDEABLE], [.SAVE_SKIP_COLLAPSED], [.SAVE_ALL] and [.SAVE_NONE].
     * @see .getSaveFlags
     * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_saveFlags
     */
    open fun setSaveFlags(@SaveFlags flags: Int) {
        this.saveFlags = flags
    }


    /**
     * Sets whether this bottom sheet is can be collapsed/expanded by dragging. Note: When disabling
     * dragging, an app will require to implement a custom way to expand/collapse the bottom sheet
     *
     * @param draggable `false` to prevent dragging the sheet to collapse and expand
     * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_draggable
     */
    open fun setDraggable(draggable: Boolean) {
        this.draggable = draggable
    }


    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden after it
     * is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    open fun setSkipCollapsed(skipCollapsed: Boolean) {
        this.skipCollapsed = skipCollapsed
    }


    /**
     * Sets whether the height of the expanded sheet is determined by the height of its contents, or
     * if it is expanded in two stages (half the height of the parent container, full height of parent
     * container). Default value is true.
     *
     * @param fitToContents whether or not to fit the expanded sheet to its contents.
     */
    open fun setFitToContents(fitToContents: Boolean) {
        if (this.fitToContents == fitToContents) {
            return
        }
        this.fitToContents = fitToContents

        // If sheet is already laid out, recalculate the collapsed offset based on new setting.
        // Otherwise, let onLayoutChild handle this later.
        if (viewRef != null) {
            calculateCollapsedOffset()
        }
        // Fix incorrect expanded settings depending on whether or not we are fitting sheet to contents.
        setStateInternal(if (this.fitToContents && state == STATE_HALF_EXPANDED) STATE_EXPANDED else state)
        updateAccessibilityActions()
    }


    /**
     * Sets whether this bottom sheet should adjust it's position based on the system gesture area on
     * Android Q and above.
     *
     *
     * Note: the bottom sheet will only adjust it's position if it would be unable to be scrolled
     * upwards because the peekHeight is less than the gesture inset margins,(because that would cause
     * a gesture conflict), gesture navigation is enabled, and this `ignoreGestureInsetBottom`
     * flag is false.
     */
    open fun setGestureInsetBottomIgnored(gestureInsetBottomIgnored: Boolean) {
        this.gestureInsetLeftIgnored = gestureInsetBottomIgnored
    }


    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * @param hideable `true` to make this bottom sheet hideable.
     * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    @JvmName("setHideable1")
    fun setHideable(hideable: Boolean) {
        if (this.hideable != hideable) {
            this.hideable = hideable
            if (!hideable && state == STATE_HIDDEN) {
                // Lift up to collapsed state
                setState(STATE_COLLAPSED)
            }
            updateAccessibilityActions()
        }
    }


    /**
     * Sets the height of the bottom sheet when it is collapsed.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels, or [     ][.PEEK_HEIGHT_AUTO] to configure the sheet to peek automatically at 16:9 ratio keyline.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    fun setPeekHeight(peekHeight: Int) {
        setPeekHeight(peekHeight, false)
    }


    /**
     * Sets the height of the bottom sheet when it is collapsed while optionally animating between the
     * old height and the new height.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels, or [     ][.PEEK_HEIGHT_AUTO] to configure the sheet to peek automatically at 16:9 ratio keyline.
     * @param animate Whether to animate between the old height and the new height.
     * @attr ref
     * com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
     */
    fun setPeekHeight(peekHeight: Int, animate: Boolean) {
        Log.v(TAG, "setPeekHeight  peekHeight $peekHeight ")
        var layout = false
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!peekHeightAuto) {
                peekHeightAuto = true
                layout = true
            }
        } else if (peekHeightAuto || this.peekHeight != peekHeight) {
            peekHeightAuto = false
            this.peekHeight = Math.max(0, peekHeight)
            layout = true
        }
        // If sheet is already laid out, recalculate the collapsed offset based on new setting.
        // Otherwise, let onLayoutChild handle this later.
        if (layout) {
            updatePeekHeight(animate)
        }
    }


    private fun createMaterialShapeDrawable(
        context: Context, attrs: AttributeSet, hasBackgroundTint: Boolean
    ) {
        this.createMaterialShapeDrawable(context, attrs, hasBackgroundTint, null)
    }

    private fun createMaterialShapeDrawable(
        context: Context,
        attrs: AttributeSet,
        hasBackgroundTint: Boolean,
        bottomSheetColor: ColorStateList?
    ) {
        if (shapeThemingEnabled) {
            this.shapeAppearanceModelDefault = ShapeAppearanceModel.builder(context, attrs, R.attr.bottomSheetStyle, DEF_STYLE_RES)
                .build()
            materialShapeDrawable = MaterialShapeDrawable(shapeAppearanceModelDefault!!)
            materialShapeDrawable!!.initializeElevationOverlay(context)
            if (hasBackgroundTint && bottomSheetColor != null) {
                materialShapeDrawable!!.fillColor = bottomSheetColor
            } else {
                // If the tint isn't set, use the theme default background color.
                val defaultColor = TypedValue()
                context.theme.resolveAttribute(android.R.attr.colorBackground, defaultColor, true)
                materialShapeDrawable!!.setTint(defaultColor.data)
            }
        }
    }


    private fun createShapeValueAnimator() {
        interpolatorAnimator = ValueAnimator.ofFloat(0f, 1f)
        interpolatorAnimator?.setDuration(CORNER_ANIMATION_DURATION.toLong())
        interpolatorAnimator?.addUpdateListener(
            AnimatorUpdateListener { animation ->
                val value = animation.animatedValue as Float
                if (materialShapeDrawable != null) {
                    materialShapeDrawable!!.interpolation = value
                }
            })
    }


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
        Log.v(TAG, "onLayoutChild")
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
                materialShapeDrawable!!.elevation = if (elevation == -1f) ViewCompat.getElevation(child) else elevation
                // 根据初始状态更新材料形状。
                isShapeExpanded = state == STATE_EXPANDED
                materialShapeDrawable!!.interpolation = if (isShapeExpanded) 0f else 1f
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
        //<editor-fold desc="更改偏移量核心代码1" >

        //TODO 顶部换右边
        val savedTop = child.right
        // 首先让父级布局
        parent.onLayoutChild(child, layoutDirection)
        // 偏移底部纸张
        parentWidth = parent.width
        parentHeight = parent.height
        childHeight = child.height

        childWidth = child.width

        fitToContentsOffset = 0.coerceAtLeast(parentWidth - childWidth)

        calculateHalfExpandedOffset()
        calculateCollapsedOffset()
        // TODO 顶部和底部换左部和右部
        if (state == STATE_EXPANDED) {
            ViewCompat.offsetLeftAndRight(child, getExpandedOffset())
        } else if (state == STATE_HALF_EXPANDED) {
            ViewCompat.offsetLeftAndRight(child, halfExpandedOffset)
        } else if (hideable && state == STATE_HIDDEN) {
            // todo 高改宽
            ViewCompat.offsetLeftAndRight(child, parentWidth)
        } else if (state == STATE_COLLAPSED) {
            ViewCompat.offsetLeftAndRight(child, collapsedOffset)
        } else if (state == STATE_DRAGGING || state == STATE_SETTLING) {
            //TODO 顶部换右边
            ViewCompat.offsetLeftAndRight(child, savedTop - child.right)
        }

        //</editor-fold>
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
                val initialX = ev.getX().toInt()
                initialY = ev.getY().toInt()
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
        // 我们必须处理 ViewDragHelper 不捕获底部工作表的情况，因为
        // 它不是其父级的最顶层视图。当触摸事件发生时，这不是必需的
        // 当嵌套滚动逻辑处理这种情况时，发生在滚动内容上。

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
        lastNestedScrollDy = 0
        nestedScrolled = false
        return axes and ViewCompat.SCROLL_AXIS_HORIZONTAL != 0 // todo 垂直改为垂直
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        Log.v(TAG, lll())
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            // Ignore fling here. The ViewDragHelper handles it.
            return
        }
        val scrollingChild = if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null
        if (target !== scrollingChild) {
            return
        }
        // TODO 顶部改右部
        val currentTop = child.right
        val newTop = currentTop - dy
        if (dy > 0) { // Upward
            if (newTop < getExpandedOffset()) {
                consumed[1] = currentTop - getExpandedOffset()
                //TODO 顶部和底部 改 左部和右部
                ViewCompat.offsetLeftAndRight(child, -consumed[1])
                setStateInternal(STATE_EXPANDED)
            } else {
                if (!draggable) {
                    // Prevent dragging
                    return
                }
                consumed[1] = dy
                //TODO 顶部和底部 改 左部和右部
                ViewCompat.offsetLeftAndRight(child, -dy)
                setStateInternal(STATE_DRAGGING)
            }
        } else if (dy < 0) { // Downward
            if (!target.canScrollVertically(-1)) {
                if (newTop <= collapsedOffset || hideable) {
                    if (!draggable) {
                        // Prevent dragging
                        return
                    }
                    consumed[1] = dy
                    //TODO 顶部和底部 改 左部和右部
                    ViewCompat.offsetLeftAndRight(child, -dy)
                    setStateInternal(STATE_DRAGGING)
                } else {
                    consumed[1] = currentTop - collapsedOffset
                    ViewCompat.offsetLeftAndRight(child, -consumed[1])
                    setStateInternal(STATE_COLLAPSED)
                }
            }
        }
        //todo 顶部换右
        dispatchOnSlide(child.right)
        lastNestedScrollDy = dy
        nestedScrolled = true
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, type: Int) {
        Log.v(TAG, lll())
        // TODO 顶部换右
        if (child.right == getExpandedOffset()) {
            setStateInternal(STATE_EXPANDED)
            return
        }
        if (nestedScrollingChildRef == null || target !== nestedScrollingChildRef!!.get() || !nestedScrolled) {
            return
        }
        val top: Int
        val targetState: Int
        if (lastNestedScrollDy > 0) {
            if (fitToContents) {
                top = fitToContentsOffset
                targetState = STATE_EXPANDED
            } else {
                // TODO 顶部换右
                val currentTop = child.right
                if (currentTop > halfExpandedOffset) {
                    top = halfExpandedOffset
                    targetState = STATE_HALF_EXPANDED
                } else {
                    top = expandedOffsetL
                    targetState = STATE_EXPANDED
                }
            }
        } else if (hideable && shouldHide(child, getYVelocity())) {

            // todo 高改宽
            top = parentWidth
            targetState = STATE_HIDDEN
        } else if (lastNestedScrollDy == 0) {
            // TODO 顶部换右
            val currentTop = child.right
            if (fitToContents) {
                if (Math.abs(currentTop - fitToContentsOffset) < Math.abs(currentTop - collapsedOffset)) {
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
                    if (Math.abs(currentTop - halfExpandedOffset) < Math.abs(currentTop - collapsedOffset)) {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    } else {
                        top = collapsedOffset
                        targetState = STATE_COLLAPSED
                    }
                }
            }
        } else {
            if (fitToContents) {
                top = collapsedOffset
                targetState = STATE_COLLAPSED
            } else {
                // Settle to nearest height.
                // TODO 顶部换右
                val currentTop = child.right
                if (Math.abs(currentTop - halfExpandedOffset) < Math.abs(currentTop - collapsedOffset)) {
                    top = halfExpandedOffset
                    targetState = STATE_HALF_EXPANDED
                } else {
                    top = collapsedOffset
                    targetState = STATE_COLLAPSED
                }
            }
        }
        startSettlingAnimation(child, targetState, top, false)
        nestedScrolled = false
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {

    }

    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float): Boolean {
        return if (nestedScrollingChildRef != null) {
            (target === nestedScrollingChildRef!!.get()
                    && (state != STATE_EXPANDED
                    || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)))
        } else {
            false
        }
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


        const val TAG = "LeftSheetBehavior"

        /**
         * A utility function to get the [BottomSheetBehavior] associated with the `view`.
         *
         * @param view The [View] with [BottomSheetBehavior].
         * @return The [BottomSheetBehavior] associated with the `view`.
         */
        @NonNull
        @JvmStatic
        fun <V : View?> from(view: V): LeftSheetBehavior<View> {
            val params = view?.layoutParams
            require(params is CoordinatorLayout.LayoutParams) { "该视图不是 CoordinatorLayout 的子视图" }

            val behavior = params.behavior
            require(behavior is LeftSheetBehavior<*>) { "The view is not associated with BottomSheetBehavior" }
            return behavior as LeftSheetBehavior
        }

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

        private var maximumVelocity = 0f


        //</editor-fold>

        //</editor-fold>
    }

    //<editor-fold desc="匿名内" >


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
            return viewRef != null && viewRef!!.get() == child
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
            // todo 高改宽
            return child.top > (parentWidth + getExpandedOffset()) / 2
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val top: Int
            @State val targetState: Int
            if (yvel < 0) { // Moving up
                if (fitToContents) {
                    top = fitToContentsOffset
                    targetState = STATE_EXPANDED
                } else {//TODO 顶部换右边
                    val currentTop = releasedChild.top
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
                ) {// todo 高改宽
                    top = parentWidth
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
                            targetState = STATE_COLLAPSED
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
            return child.top
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {

            Log.d(TAG, " left $left  getExpandedOffset  ${getExpandedOffset()}  hideable : $hideable   parentWidth :  $parentWidth   collapsedOffset : $collapsedOffset")
            return MathUtils.clamp(   //todo 高改宽
                left, getExpandedOffset(), if (hideable) parentWidth else collapsedOffset
            )
        }


        /**
         * Return the magnitude of a draggable child view's horizontal range of motion in pixels.
         * This method should return 0 for views that cannot move horizontally.
         *
         * @param child Child view to check
         * @return range of horizontal motion in pixels
         */
        override fun getViewHorizontalDragRange(child: View): Int {
            return if (hideable) {
                // todo 高改宽
                parentWidth
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

    private fun getYVelocity(): Float {
        if (velocityTracker == null) {
            return 0f
        }
        velocityTracker!!.computeCurrentVelocity(1000, maximumVelocity)
        return velocityTracker!!.getYVelocity(activePointerId)
    }

    private fun reset() {
        activePointerId = ViewDragHelper.INVALID_POINTER
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
    }


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
                    gestureInsetLeft = insets.mandatorySystemGestureInsets.bottom
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
        Log.v(TAG, "updatePeekHeight  更新透视高度  $state ")
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
        Log.v(TAG, lll())
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

    //<editor-fold desc="核心代码2" >

    //<editor-fold desc="计算半扩展偏移" >
    private fun calculateHalfExpandedOffset() {
        // todo 高改宽
        halfExpandedOffset = (parentWidth * (1 - halfExpandedRatio)).toInt()
    }
    //</editor-fold>

    /**
     * 计算折叠偏移
     */
    private fun calculateCollapsedOffset() {
        val peek: Int = calculatePeekHeight()
        if (fitToContents) {
            // todo 高改宽            //todo 整数转负数
            collapsedOffset = Math.max(parentWidth - peek, fitToContentsOffset).negate()
        } else {
            // todo 高改宽            //todo 整数转负数
            collapsedOffset = (parentWidth - peek).negate()
        }
    }

    /**
     * 计算窥视高度
     */
    private fun calculatePeekHeight(): Int {
        if (peekHeightAuto) {
            // 改 val desiredHeight = Math.max(peekHeightMin, parentHeight - parentWidth * 9 / 16)
            val desiredHeight = Math.max(peekHeightMin, parentWidth - parentHeight * 16 / 9)
            // todo 高改宽
            return Math.min(desiredHeight, childWidth)
        }
        return if (!gestureInsetLeftIgnored && gestureInsetLeft > 0) {
            Math.max(peekHeight, gestureInsetLeft + peekHeightGestureInsetBuffer)
        } else peekHeight
    }

    //</editor-fold>


    //<editor-fold desc="更新重要的辅助功能" >
    private fun updateImportantForAccessibility(expanded: Boolean) {
        Log.v(TAG, lll() + " expanded : $expanded")
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
            if (child == viewRef!!.get()) {
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
        Log.v(TAG, lll())
        if (state == STATE_SETTLING) {
            // 特殊情况：我们想知道我们正在解决哪个状态，所以等待另一个调用。
            return
        }
        val expand = state == STATE_EXPANDED
        if (isShapeExpanded != expand) {
            isShapeExpanded = expand
            if (materialShapeDrawable != null && interpolatorAnimator != null) {
                if (interpolatorAnimator!!.isRunning()) {
                    interpolatorAnimator!!.reverse()
                } else {
                    val to = if (expand) 0f else 1f
                    val from = 1f - to
                    interpolatorAnimator!!.setFloatValues(from, to)
                    interpolatorAnimator!!.start()
                }
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="更新辅助功能操作" >
    private fun updateAccessibilityActions() {
        Log.v(TAG, lll())
        Log.v(TAG, "_________________________________________")
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
            val slideOffset =                                                                                             // todo 高改宽
                if (top > collapsedOffset || collapsedOffset == getExpandedOffset()) (collapsedOffset - top).toFloat() / (parentWidth - collapsedOffset) else (collapsedOffset - top).toFloat() / (collapsedOffset - getExpandedOffset())
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
        Log.v(TAG, "setState")
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
        Log.v(TAG, lll())
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
            //todo 高改宽
            top = parentWidth
        } else {
            throw IllegalArgumentException("非法状态论证: $state")
        }
        startSettlingAnimation(child, state, top, false)
    }

    /**
     * 开始稳定定动画
     */
    fun startSettlingAnimation(child: View, state: Int, top: Int, settleFromViewDragHelper: Boolean) {
        Log.v(TAG, lll())
        val startedSettling = (viewDragHelper != null
                //TODO  超级核心代码 实现 自动上下或则左右展开
                //if (settleFromViewDragHelper) viewDragHelper!!.settleCapturedViewAt(child.left, top) else viewDragHelper!!.smoothSlideViewTo(child, child.left, top))
                && if (settleFromViewDragHelper) viewDragHelper!!.settleCapturedViewAt(child.bottom, top) else viewDragHelper!!.smoothSlideViewTo(child, child.top, top))
        Log.v(TAG, "startSettlingAnimation startedSettling : $startedSettling")
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

    /**
     * 设置内部状态
     */
    fun setStateInternal(@State state: Int) {
        Log.v(TAG, lll())
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
