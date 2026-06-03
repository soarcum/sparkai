package com.soar.sparkai.feature.floatwindow.service

/**
 * AAMS (AI-Assisted Module System) 坐标高亮标注边界实体
 */
data class HighlightBound(
    val originalValue: String,
    val calculatedValue: String,
    val ymin: Float,
    val xmin: Float,
    val ymax: Float,
    val xmax: Float
)
