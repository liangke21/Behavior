package com.example.myBehavior.internal

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


object ViewUtils {
    /**
     * 环绕androidx.core.view.OnApplyWindowInsetsListener包装器，用于记录视图的初始填充和附加时应用插入的请求。
     */
    @JvmStatic
    fun doOnApplyWindowInsets(view: View, listener: OnApplyWindowInsetsListener) {
        // 创建视图填充状态的快照。
        val initialPadding = RelativePadding(
            ViewCompat.getPaddingStart(view),
            view.paddingTop,//TODO 顶部换右边 view.paddingTop,
            ViewCompat.getPaddingEnd(view),
            view.paddingBottom //TODO 左边换底部 view.paddingBottom
        )

        // 设置一个实际的 OnApplyWindowInsetsListener 代理给定的回调，也传递
        // 在原始填充状态。
        ViewCompat.setOnApplyWindowInsetsListener(view) { view2, insets -> listener.onApplyWindowInsets(view2, insets, RelativePadding(initialPadding)) }
        // 请求一些插图。
        requestApplyInsetsWhenAttached(view)

    }

    /** 一旦附加，应将插入的请求应用于此视图。  */
    @JvmStatic
    fun requestApplyInsetsWhenAttached(view: View) {
        if (ViewCompat.isAttachedToWindow(view)) {
            // We're already attached, just request as normal.
            ViewCompat.requestApplyInsets(view)
        } else {
            // We're not attached to the hierarchy, add a listener to request when we are.
            view.addOnAttachStateChangeListener(
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        v.removeOnAttachStateChangeListener(this)
                        ViewCompat.requestApplyInsets(v)
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                })
        }
    }

    /**
     * 环绕androidx.core.view.OnApplyWindowInsetsListener包装器，它还传递视图上的初始填充集。
     * 与doOnApplyWindowInsets(View, ViewUtils.OnApplyWindowInsetsListener) 。
     */
    interface OnApplyWindowInsetsListener {

        fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat, initialPadding: RelativePadding): WindowInsetsCompat
    }

    /**
     * 用于存储视图初始填充的简单数据对象
     */
    class RelativePadding {
        var start = 0
        var top = 0
        var end = 0
        var bottom = 0

        constructor(start: Int, top: Int, end: Int, bottom: Int) {
            this.start = start
            this.top = top
            this.end = end
            this.bottom = bottom
        }


        constructor(other: RelativePadding) {
            start = other.start
            top = other.top
            end = other.end
            bottom = other.bottom
        }

        /**
         * 将此相对填充应用于视图
         */
        fun applyToView(view: View?) {
            ViewCompat.setPaddingRelative(view!!, start, top, end, bottom)
        }

    }


}