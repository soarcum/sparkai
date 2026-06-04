package com.soar.sparkai.feature.floatwindow.service

/**
 * AAMS 博彩抽水（庄家利润 margin）配对实体
 *
 * AI 仅负责识别并按盘口分组返回 odds（两向/三向），抽水 margin 由 App 统一计算，
 * 不信任 AI 算术。bbox 为该组盘口所在行的 0-100 相对百分比坐标，用于原位标注。
 */
data class RakePair(
    val label: String,
    val odds: List<Double>,
    val margin: Double,
    val ymin: Float,
    val xmin: Float,
    val ymax: Float,
    val xmax: Float
)
