package com.tracers.trace.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * TRACE 앱 아이콘 모음.
 * 디자인 HTML의 SVG path를 ImageVector로 변환.
 */
object TraceIcons {

    /** TRACE 로고 아이콘 (비행기 궤적: 곡선 + 원) */
    val TraceLogo: ImageVector by lazy {
        ImageVector.Builder(
            name = "TraceLogo",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            // 궤적 곡선
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeAlpha = 0.9f
            ) {
                // M3 16 C 8 15, 11 10, 15.5 5
                moveTo(3f, 16f)
                curveTo(8f, 15f, 11f, 10f, 15.5f, 5f)
            }
            // 비행기 원
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                // circle cx="15.8" cy="4.7" r="1.7"
                moveTo(17.5f, 4.7f)
                arcTo(1.7f, 1.7f, 0f, true, true, 14.1f, 4.7f)
                arcTo(1.7f, 1.7f, 0f, true, true, 17.5f, 4.7f)
            }
        }.build()
    }

    /** 전비 아이콘 (게이지) */
    val Efficiency: ImageVector by lazy {
        ImageVector.Builder(
            name = "Efficiency",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M12 3a9 9 0 00-9 9h3M21 12a9 9 0 00-9-9M12 12l4-4M18 12h3
                moveTo(12f, 3f)
                arcToRelative(9f, 9f, 0f, false, false, -9f, 9f)
                horizontalLineToRelative(3f)
                moveTo(21f, 12f)
                arcToRelative(9f, 9f, 0f, false, false, -9f, -9f)
                moveTo(12f, 12f)
                lineToRelative(4f, -4f)
                moveTo(18f, 12f)
                horizontalLineToRelative(3f)
            }
        }.build()
    }

    /** 정비 아이콘 (렌치) */
    val Maintenance: ImageVector by lazy {
        ImageVector.Builder(
            name = "Maintenance",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M14.5 6.5a4 4 0 00-5 5L4 17l3 3 5.5-5.5a4 4 0 005-5l-2.5 2.5-2.5-.5-.5-2.5z
                moveTo(14.5f, 6.5f)
                arcToRelative(4f, 4f, 0f, false, false, -5f, 5f)
                lineTo(4f, 17f)
                lineToRelative(3f, 3f)
                lineToRelative(5.5f, -5.5f)
                arcToRelative(4f, 4f, 0f, false, false, 5f, -5f)
                lineToRelative(-2.5f, 2.5f)
                lineToRelative(-2.5f, -0.5f)
                lineToRelative(-0.5f, -2.5f)
                close()
            }
        }.build()
    }

    /** 코인 아이콘 (별) */
    val Coin: ImageVector by lazy {
        ImageVector.Builder(
            name = "Coin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M12 3l2.4 6.6L21 12l-6.6 2.4L12 21l-2.4-6.6L3 12l6.6-2.4z
                moveTo(12f, 3f)
                lineToRelative(2.4f, 6.6f)
                lineTo(21f, 12f)
                lineToRelative(-6.6f, 2.4f)
                lineTo(12f, 21f)
                lineToRelative(-2.4f, -6.6f)
                lineTo(3f, 12f)
                lineToRelative(6.6f, -2.4f)
                close()
            }
        }.build()
    }

    /** 지역 아이콘 (핀) */
    val Region: ImageVector by lazy {
        ImageVector.Builder(
            name = "Region",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M12 21s-6-5.5-6-11a6 6 0 0112 0c0 5.5-6 11-6 11z
                moveTo(12f, 21f)
                reflectiveCurveToRelative(-6f, -5.5f, -6f, -11f)
                arcToRelative(6f, 6f, 0f, false, true, 12f, 0f)
                curveToRelative(0f, 5.5f, -6f, 11f, -6f, 11f)
                close()
                // M12 12a2 2 0 100-4 2 2 0 000 4z
                moveTo(12f, 12f)
                arcToRelative(2f, 2f, 0f, true, false, 0f, -4f)
                arcToRelative(2f, 2f, 0f, false, false, 0f, 4f)
                close()
            }
        }.build()
    }

    /** 셰브런 (오른쪽 화살표) */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M9 6l6 6-6 6
                moveTo(9f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(-6f, 6f)
            }
        }.build()
    }

    /** 스피커 아이콘 (음성 안내) */
    val Speaker: ImageVector by lazy {
        ImageVector.Builder(
            name = "Speaker",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M4 10v4h3.5l4.5 4V6L7.5 10z
                moveTo(4f, 10f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(3.5f)
                lineToRelative(4.5f, 4f)
                verticalLineTo(6f)
                lineTo(7.5f, 10f)
                close()
                // M16 9.5a3.5 3.5 0 010 5
                moveTo(16f, 9.5f)
                arcToRelative(3.5f, 3.5f, 0f, false, true, 0f, 5f)
            }
        }.build()
    }

    /** 해 아이콘 (라이트 모드) */
    val Sun: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sun",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // 원 (cx=12, cy=12, r=4) - arcTo로 그림
                moveTo(16f, 12f)
                arcTo(4f, 4f, 0f, true, true, 8f, 12f)
                arcTo(4f, 4f, 0f, true, true, 16f, 12f)
                // 광선들
                moveTo(12f, 2.5f)
                verticalLineToRelative(2f)
                moveTo(12f, 19.5f)
                verticalLineToRelative(2f)
                moveTo(2.5f, 12f)
                horizontalLineToRelative(2f)
                moveTo(19.5f, 12f)
                horizontalLineToRelative(2f)
                moveTo(5.3f, 5.3f)
                lineToRelative(1.4f, 1.4f)
                moveTo(17.3f, 17.3f)
                lineToRelative(1.4f, 1.4f)
                moveTo(5.3f, 18.7f)
                lineToRelative(1.4f, -1.4f)
                moveTo(17.3f, 6.7f)
                lineToRelative(1.4f, -1.4f)
            }
        }.build()
    }

    /** 달 아이콘 (다크 모드) */
    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z
                moveTo(20f, 14.5f)
                arcTo(8f, 8f, 0f, false, true, 9.5f, 4f)
                arcToRelative(8f, 8f, 0f, true, false, 10.5f, 10.5f)
                close()
            }
        }.build()
    }

    /** 번개 아이콘 (충전) */
    val Bolt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M13 3L5 14h6l-1 7 8-11h-6z
                moveTo(13f, 3f)
                lineTo(5f, 14f)
                horizontalLineToRelative(6f)
                lineToRelative(-1f, 7f)
                lineToRelative(8f, -11f)
                horizontalLineToRelative(-6f)
                close()
            }
        }.build()
    }

    /** 뒤로 가기 아이콘 */
    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 12f)
                horizontalLineTo(5f)
                moveTo(12f, 19f)
                lineToRelative(-7f, -7f)
                lineToRelative(7f, -7f)
            }
        }.build()
    }

    /** 왼쪽 셰브런 (뒤로) */
    val ChevronLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M15 5l-7 7 7 7 (HTML 디자인 값)
                moveTo(15f, 5f)
                lineToRelative(-7f, 7f)
                lineToRelative(7f, 7f)
            }
        }.build()
    }

    /** 닫기 아이콘 (X) */
    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // M6 6l12 12M18 6L6 18
                moveTo(6f, 6f)
                lineToRelative(12f, 12f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }.build()
    }

    /** 정보 아이콘 (i) */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // circle cx=12 cy=12 r=9
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, true, true, 3f, 12f)
                arcTo(9f, 9f, 0f, true, true, 21f, 12f)
                // M12 11v5
                moveTo(12f, 11f)
                verticalLineToRelative(5f)
                // M12 8h.01
                moveTo(12f, 8f)
                horizontalLineToRelative(0.01f)
            }
        }.build()
    }

    /** 도움말 아이콘 (원 + 물음표). CoinDark.dc.html "적립 기준 보기" */
    val Help: ImageVector by lazy {
        ImageVector.Builder(
            name = "Help",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // circle cx=12 cy=12 r=9
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, true, true, 3f, 12f)
                arcTo(9f, 9f, 0f, true, true, 21f, 12f)
                // M9.6 9.3a2.5 2.5 0 014.8.9c0 1.7-2.4 2.2-2.4 3.6
                moveTo(9.6f, 9.3f)
                arcToRelative(2.5f, 2.5f, 0f, false, true, 4.8f, 0.9f)
                curveToRelative(0f, 1.7f, -2.4f, 2.2f, -2.4f, 3.6f)
                // M12 17h.01
                moveTo(12f, 17f)
                horizontalLineToRelative(0.01f)
            }
        }.build()
    }

    /** 네 갈래 별 (채움). CoinDark 1km 칸 */
    val Star4: ImageVector by lazy {
        ImageVector.Builder(
            name = "Star4",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black), stroke = null) {
                // M12 3l2.4 6.6L21 12l-6.6 2.4L12 21l-2.4-6.6L3 12l6.6-2.4z
                moveTo(12f, 3f)
                lineToRelative(2.4f, 6.6f)
                lineTo(21f, 12f)
                lineToRelative(-6.6f, 2.4f)
                lineTo(12f, 21f)
                lineToRelative(-2.4f, -6.6f)
                lineTo(3f, 12f)
                lineToRelative(6.6f, -2.4f)
                close()
            }
        }.build()
    }
}
