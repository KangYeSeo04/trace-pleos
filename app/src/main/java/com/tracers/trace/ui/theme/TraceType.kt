package com.tracers.trace.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.tracers.trace.R

/**
 * Pretendard 폰트 패밀리.
 * 400(Regular), 500(Medium), 600(SemiBold), 700(Bold).
 */
val PretendardFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),   // 400
    Font(R.font.pretendard_medium, FontWeight.Medium),    // 500
    Font(R.font.pretendard_semibold, FontWeight.SemiBold), // 600
    Font(R.font.pretendard_bold, FontWeight.Bold),        // 700
)

/**
 * Inter 폰트 패밀리.
 * 숫자(특히 타일의 큰 숫자)에 사용.
 * 400(Regular), 500(Medium), 600(SemiBold), 700(Bold).
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),   // 400
    Font(R.font.inter_medium, FontWeight.Medium),    // 500
    Font(R.font.inter_semibold, FontWeight.SemiBold), // 600
    Font(R.font.inter_bold, FontWeight.Bold),        // 700
)

/**
 * TRACE 타이포그래피.
 *
 * 기본 글자 간격: -0.01em (letterSpacing = -0.01.em은 sp 단위 변환 필요).
 * 숫자는 고정폭: fontFeatureSettings = "tnum".
 */
@Immutable
data class TraceType(
    /** 본문용 폰트 (Pretendard) */
    val fontFamily: FontFamily = PretendardFontFamily,

    /** 숫자용 폰트 (Inter) */
    val numberFontFamily: FontFamily = InterFontFamily,

    /** 기본 글자 간격 (-0.01em ≈ -0.16sp at 16sp base) */
    val defaultLetterSpacing: Float = -0.01f,

    /** 숫자 글자 간격 (-0.035em, 디자인 HTML 기준) */
    val numberLetterSpacing: Float = -0.035f,
) {
    /** 기본 스타일 생성 (크기 지정, 간격 자동 적용) */
    fun body(sizeSp: Float, weight: FontWeight = FontWeight.Normal): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = sizeSp.sp,
        letterSpacing = (sizeSp * defaultLetterSpacing).sp,
    )

    /** 숫자용 스타일 (Inter, 고정폭 tnum) */
    fun number(sizeSp: Float, weight: FontWeight = FontWeight.SemiBold): TextStyle = TextStyle(
        fontFamily = numberFontFamily,
        fontWeight = weight,
        fontSize = sizeSp.sp,
        letterSpacing = (sizeSp * numberLetterSpacing).sp,
        fontFeatureSettings = "tnum",
    )

    /** 큰 숫자용: CSS line-height: 1 (글자 위아래 여백을 잘라 크기와 같은 높이) */
    fun numberTight(sizeSp: Float, weight: FontWeight = FontWeight.SemiBold): TextStyle =
        number(sizeSp, weight).copy(
            lineHeight = sizeSp.sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        )
}

val LocalTraceType = staticCompositionLocalOf { TraceType() }
