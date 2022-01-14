package com.liangke.viewpoint.behavior

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
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
    private var state: State = STATE_EXPANDED //状态

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.GlobalBehavior)
        val direction = a.getInt(R.styleable.GlobalBehavior_direction, 1)
        setFoldDirection(Direction.values()[direction - 1])
        val dimensionHeight = a.getDimension(R.styleable.GlobalBehavior_gb_peekHeight, -1F)
        setPeekHeight(dimensionHeight)
        a.recycle()
    }

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

        calculateCollapsedOffset()
        handlingFoldOrientation(child)

        return true
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
            TODO("Not yet implemented")
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