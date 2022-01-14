package com.liangke.viewpoint.enum

enum class State {


    /**
     * 工作表已展开。
     */
    STATE_EXPANDED,

    /**
     * 工作表已折叠
     */
    STATE_COLLAPSED,

    /**
     * 工作表被隐藏。
     *
     */
    STATE_HIDDEN,

    /**
     * 左表是半展开的（当 mFitToContents 为 false 时使用）。
     */
    STATE_HALF_EXPANDED
}