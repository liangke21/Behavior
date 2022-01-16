package com.liangke.viewpoint.behavior

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.Nullable
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import com.liangke.viewpoint.R
import com.liangke.viewpoint.enum.Direction
import com.liangke.viewpoint.enum.Direction.*
import com.liangke.viewpoint.enum.State
import com.liangke.viewpoint.enum.State.*
import java.lang.ref.WeakReference
import kotlin.math.abs

class GlobalBehavior<V : View> : CoordinatorLayout.Behavior<V> {


    private var ignoreEvents = false
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

    private var _viewRef: WeakReference<V>? = null
    private val viewRef get() = _viewRef!!
    var viewDragHelper: ViewDragHelper? = null //查看拖动助手
    private var settleRunnable: SettleRunnable? = null //解决 Runnable

    private var isHideInvalidCollapsed = true//隐藏状态时折叠状态是否失效
    private var state: State = STATE_COLLAPSED //状态
    private var initialY = 0 //初始化y
    private var initialX = 0 //初始化X
    private var touchingScrollingChild = false//触摸滚动的子布局
    var nestedScrollingChildRef: WeakReference<View>? = null //嵌套滚动子引用
    private var draggable = true //可拖动的
    private var activePointerId = 0//活动指针

    private var velocityTracker: VelocityTracker? = null//速度追踪器
    private var maximumVelocity = 0f

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.GlobalBehavior)
        val direction = a.getInt(R.styleable.GlobalBehavior_direction, 1)
        setFoldDirection(Direction.values()[direction - 1])
        val dimensionHeight = a.getDimension(R.styleable.GlobalBehavior_gb_peekHeight, -1F)
        setPeekHeight(dimensionHeight)
        setDraggable(a.getBoolean(R.styleable.GlobalBehavior_gb_draggable, true))
        a.recycle()
        val configuration = ViewConfiguration.get(context)
        maximumVelocity = configuration.scaledMaximumFlingVelocity.toFloat()
    }

    fun setDraggable(b: Boolean) {
        this.draggable = b
    }

    /**
     * 设置窥视高度
     * @param dimensionHeight Float
     */
    fun setPeekHeight(dimensionHeight: Float) {
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

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        _viewRef = null
        viewDragHelper = null
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.fitsSystemWindows = true
        }
        if (_viewRef == null) {
            _viewRef = WeakReference(child)
        }

        if (ViewCompat.getImportantForAccessibility(child)
            == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        ) {
            ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
        }
        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, dragCallback)
        }

        parent.onLayoutChild(child, layoutDirection)

        parentWidth = parent.width
        parentHeight = parent.height

        childWidth = child.width
        childHeight = child.height

        Log.d(TAG, "parentWidth $parentWidth parentHeight $parentHeight childWidth $childWidth childHeight $childHeight")
        calculateCollapsedOffset()
        handlingFoldOrientation(child)
        nestedScrollingChildRef = WeakReference<View>(findScrollingChild(child))
        return true
    }

    /**
     * 找到滚动的孩子
     * @param view View?
     * @return View?
     */
    fun findScrollingChild(view: View?): View? {
        if (ViewCompat.isNestedScrollingEnabled(view!!)) {
            return view
        }
        if (view is ViewGroup) {
            var i = 0
            val count = view.childCount
            while (i < count) {
                val scrollingChild = findScrollingChild(view.getChildAt(i))
                if (scrollingChild != null) {
                    return scrollingChild
                }
                i++
            }
        }
        return null
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        if (!child.isShown || !draggable) {
            return false
        }
        val action: Int = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) { //按下
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)
        when (action) {


            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                touchingScrollingChild = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                if (ignoreEvents) {
                    ignoreEvents = false
                    return false
                }

            }
            MotionEvent.ACTION_DOWN -> {
                initialY = ev.y.toInt()
                initialX = ev.x.toInt()
                val scrollingChild = if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null
                if (scrollingChild != null && parent.isPointInChildBounds(scrollingChild, initialX, initialY)) {
                    activePointerId = ev.getPointerId(ev.actionIndex)
                    touchingScrollingChild = true
                }

                ignoreEvents = (activePointerId == MotionEvent.INVALID_POINTER_ID
                        && !parent.isPointInChildBounds(child, initialX, initialY))
            }
        }
        if (!ignoreEvents && viewDragHelper!!.shouldInterceptTouchEvent(ev)) {
            return true
        }

        val scroll = if (nestedScrollingChildRef != null) nestedScrollingChildRef!!.get() else null

        return (action == MotionEvent.ACTION_MOVE && scroll != null && !ignoreEvents
                && !parent.isPointInChildBounds(scroll, ev.x.toInt(), ev.y.toInt())
                && viewDragHelper != null && abs(initialY - ev.y) > viewDragHelper!!.touchSlop)

    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        if (!child.isShown) {
            return false
        }

        val action: Int = ev.actionMasked

        viewDragHelper?.processTouchEvent(ev)

        if (action == MotionEvent.ACTION_DOWN) {
            reset()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(ev)

        //  Log.d("onTouchEvent ", "${viewDragHelper != null} ${action == MotionEvent.ACTION_MOVE } ${!ignoreEvents} ")
        if (viewDragHelper != null && action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            minimalDrag(ev.x, ev.y) {
                viewDragHelper!!.captureChildView(child, ev.getPointerId(ev.actionIndex))
            }
        }

        return !ignoreEvents
    }

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
     * 最小距离拖动
     * @param x Float
     * @param y Float
     * @param function Function0<Unit>
     */
    private fun minimalDrag(x: Float, y: Float, @Nullable function: () -> Unit) {
        Log.d(TAG, "最小拖动距离 x $x  y $y")

        if (abs(initialY - y.toInt()) > viewDragHelper!!.touchSlop) {
            function()
        }
        if (abs(initialX - x.toInt()) > viewDragHelper!!.touchSlop) {
            function()
        }
    }


    private fun calculateCollapsedOffset() {

        collapsedOffset = when (direction) {
            BOTTOM_SHEET -> {
                childHeight - peekHeight
            }
            TOP_SHEET -> {
                -(childHeight - peekHeight)
            }
            LEFT_SHEET -> {
                -(childWidth - peekHeight)
            }
            RIGHT_SHEET -> {
                (childWidth - peekHeight)
            }
        }
    }

    /**
     * 处理折叠方向
     */
    private fun handlingFoldOrientation(child: View) {
        when (direction) {
            BOTTOM_SHEET -> {
                ViewCompat.offsetTopAndBottom(child, collapsedOffset)
            }
            TOP_SHEET -> {
                ViewCompat.offsetTopAndBottom(child, collapsedOffset)
            }
            LEFT_SHEET -> {
                ViewCompat.offsetLeftAndRight(child, collapsedOffset)
            }
            RIGHT_SHEET -> {
                ViewCompat.offsetLeftAndRight(child, collapsedOffset)
            }
        }

    }

    /**
     * 当进入隐藏状态是否失效  [State]== STATE_COLLAPSED 状态
     * 默认为 失效 true
     * @param b Boolean
     */
    fun setHideInvalidCollapsed(b: Boolean) {
        isHideInvalidCollapsed = b
    }

    /**
     * 设置状态
     * @param state State
     */
    fun setState(state: State) {
        val child = viewRef.get() ?: return
        Log.d(TAG, "setState")
        setTleToState(child, state)
    }

    private fun setTleToState(child: View, state: State) {
        var swipeDirection = 0
        swipeDirection = when (state) {
            STATE_EXPANDED -> {
                if (direction == TOP_SHEET) {
                    collapsedOffset + (childHeight - peekHeight)
                } else if (direction == BOTTOM_SHEET) {
                    collapsedOffset - (childHeight - peekHeight)
                } else if (direction == LEFT_SHEET) {
                    collapsedOffset + (childWidth - peekHeight)
                } else {
                    collapsedOffset - (childWidth - peekHeight)
                }
            }
            STATE_COLLAPSED -> {
                if (this.state == STATE_HIDDEN && isHideInvalidCollapsed) {
                    return
                }
                if (direction == TOP_SHEET) {
                    collapsedOffset
                } else if (direction == BOTTOM_SHEET) {
                    collapsedOffset
                } else if (direction == LEFT_SHEET) {
                    collapsedOffset
                } else {
                    collapsedOffset
                }
            }
            STATE_HIDDEN -> {
                if (direction == TOP_SHEET) {
                    collapsedOffset - peekHeight
                } else if (direction == BOTTOM_SHEET) {
                    collapsedOffset + peekHeight
                } else if (direction == LEFT_SHEET) {
                    collapsedOffset - peekHeight
                } else {
                    collapsedOffset + peekHeight
                }
            }
            STATE_HALF_EXPANDED -> {
                if (direction == TOP_SHEET) {
                    collapsedOffset + (childHeight / 2 - peekHeight)
                } else if (direction == BOTTOM_SHEET) {
                    collapsedOffset - (childHeight / 2 - peekHeight)
                } else if (direction == LEFT_SHEET) {
                    collapsedOffset + (childWidth / 2 - peekHeight)
                } else {
                    collapsedOffset - (childWidth / 2 - peekHeight)
                }

            }
        }
        this.state = state
        Log.d(TAG, "swipeDirection $swipeDirection")
        startSettlingAnimation(child, state, swipeDirection, false)
    }


    private val dragCallback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (touchingScrollingChild) {
                return false
            }
            return child == viewRef.get()
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            Log.v(TAG, "匿名类 onViewPositionChanged   dx  $dx dy  $dy")
            super.onViewPositionChanged(changedView, left, top, dx, dy)
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {

            Log.v(TAG, "匿名类 onViewReleased   dx  $xvel dy  $yvel ")
            super.onViewReleased(releasedChild, xvel, yvel)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            if (direction == TOP_SHEET) {
                return MathUtils.clamp(top, -parentHeight, 0)
            }
            if (direction == BOTTOM_SHEET) {
                return MathUtils.clamp(top, 0, parentHeight)
            }
            return super.clampViewPositionVertical(child, top, dy)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            if (direction == LEFT_SHEET) {
                return MathUtils.clamp(left, -parentWidth, 0)
            }
            if (direction == RIGHT_SHEET) {
                return MathUtils.clamp(left, 0, parentWidth)
            }
            return super.clampViewPositionHorizontal(child, left, dx)
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

    private inner class SettleRunnable(private val view: View) : Runnable {

        var isPosted = false
        override fun run() {
            if (viewDragHelper!!.continueSettling(true)) {
                ViewCompat.postOnAnimation(view, this)
            }
            isPosted = false
        }
    }

    /**
     * 稳定动画
     * @param child View
     * @param state State
     * @param swipeDirection Int
     * @param b Boolean
     */
    private fun startSettlingAnimation(child: View, state: State, swipeDirection: Int, b: Boolean) {
        val startedSettling = (viewDragHelper != null
                &&
                if (b) {
                    viewDragHelper!!.settleCapturedViewAt(child.left, swipeDirection)
                } else {
                    if (direction == RIGHT_SHEET || direction == LEFT_SHEET) {
                        viewDragHelper!!.smoothSlideViewTo(child, swipeDirection, child.top)
                    } else {
                        viewDragHelper!!.smoothSlideViewTo(child, child.left, swipeDirection)
                    }
                })

        if (startedSettling) {
            Log.d(TAG, "动画是否通过 $startedSettling   偏移房方向 $swipeDirection  合适类容 $collapsedOffset")

            if (settleRunnable == null) {
                settleRunnable = SettleRunnable(child)
            }
            if (!settleRunnable!!.isPosted) {
                ViewCompat.postOnAnimation(child, settleRunnable!!)
                settleRunnable!!.isPosted = true
            }
        }

    }
}