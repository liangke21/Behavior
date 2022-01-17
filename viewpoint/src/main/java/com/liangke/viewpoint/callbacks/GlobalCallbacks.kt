package com.liangke.viewpoint.callbacks

import android.view.View
import com.liangke.viewpoint.behavior.GlobalBehavior
import com.liangke.viewpoint.enum.State

abstract class GlobalCallbacks {

    /**
     * 当全局工作表更改其状态时调用
     *
     * @param globalBehavior 全局工作表视图。
     * @param State 新状态。这将是其中之一 [.STATE_EXPANDED], [     ][.STATE_COLLAPSED], [.STATE_HIDDEN], [.STATE_HALF_EXPANDED].
     */
    abstract fun onStateChanged(globalBehavior: View, state: State)

    /**
     * 在拖动底部工作表时调用。
     *
     * @param globalBehavior 全局工作表视图。
     * @param offset 此全局工作表在 [正数,负数] 范围内的新偏移量。偏移量增加
     *  top 负数偏移 and bottom 正数偏移
     *  left 负数偏移 and right 正数偏移
     */
    abstract fun onSlide(globalBehavior: View, offset: Int)

}