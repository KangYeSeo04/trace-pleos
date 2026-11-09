package com.tracers.trace.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * TRACE 다크 테마 색상 토큰.
 * CLAUDE.md의 "다크 테마 토큰" 표 기준.
 */
@Immutable
data class TraceColors(
    /** 화면 바탕 */
    val panel: Color = Color(0xFF17191D),
    /** 카드·타일 바탕 */
    val tile: Color = Color(0xFF202328),
    /** 주요 글자 */
    val t1: Color = Color(0xFFF2F3F5),
    /** 보조 글자 */
    val t2: Color = Color(0xFFA3A9B2),
    /** 흐린 글자, 라벨 */
    val t3: Color = Color(0xFF7C828B),
    /** 구분선 */
    val line: Color = Color(0xFF2C3036),
    /** 막대 바탕 */
    val track: Color = Color(0xFF33373E),
    /** 점검 항목 막대 바탕 (MaintDark track) */
    val itemTrack: Color = Color(0xFF363A41),
    /** 점검 항목 여유 막대 (MaintDark ok, 75% 미만 회색) */
    val barNeutral: Color = Color(0xFF6B7179),
    /** 강조 파랑 (유일한 강조색) */
    val accent: Color = Color(0xFF4C9AFF),
    /** 강조 파랑 반투명 (칩 배경) */
    val accentSoft: Color = Color(0x294C9AFF), // rgba(76,154,255,0.16)
    /** 강조 파랑 옅은 바탕 (MaintDark 방문 감지 카드 accentSoft rgba(76,154,255,0.12)) */
    val accentCard: Color = Color(0x1F4C9AFF),
    /** 좋음 */
    val good: Color = Color(0xFF38C08A),
    /** 좋음 반투명 (칩 배경) */
    val goodSoft: Color = Color(0x2438C08A), // rgba(56,192,138,0.14)
    /** 주의 */
    val caution: Color = Color(0xFFF0A33C),
    /** 주의 반투명 (칩 배경). goodSoft와 같은 0.14 비율 */
    val cautionSoft: Color = Color(0x24F0A33C), // rgba(240,163,60,0.14)
    /** 경고 */
    val warn: Color = Color(0xFFEF7A70),
    /** 코인·별 */
    val gold: Color = Color(0xFFE9B949),
    /** 깨끗한 km 칸 바탕 (CoinDark cellOn rgba(233,185,73,0.14)) */
    val goldSoft: Color = Color(0x24E9B949),
    /** 위험 km 칸 바탕 (CoinDark cellOff) */
    val cellOff: Color = Color(0xFF2A2E34),
    /** 위험 km 흐린 별 (CoinDark dim) */
    val dim: Color = Color(0xFF555B63),
    /** 히어로 사진이 뜨기 전 바탕 (CoinDark heroBg) */
    val heroBg: Color = Color(0xFF050608),
    /** 밤하늘 비행운 (CoinDark 캔버스 tc '222,230,236') */
    val heroTrail: Color = Color(0xFFDEE6EC),
    /** 로고·상점 아이콘 바탕 그라데이션 위 (linear-gradient #1B2533 → #070A0F) */
    val logoBgTop: Color = Color(0xFF1B2533),
    /** 로고·상점 아이콘 바탕 그라데이션 아래 */
    val logoBgBottom: Color = Color(0xFF070A0F),
    /** 히어로 위 글자 (사진 위라 테마와 무관하게 흰색) */
    val heroText: Color = Color(0xFFFFFFFF),
    /** 히어로 하단 어둡게 (linear-gradient 끝 rgba(0,0,0,0.6)) */
    val heroScrim: Color = Color(0x99000000),
    /** 히어로 위 흰 버튼 바탕 */
    val heroButton: Color = Color(0xFFFFFFFF),
    /** 히어로 위 흰 버튼 글자 */
    val heroButtonText: Color = Color(0xFF111418),
    /** 안내 창 뒤 어둡게 (rgba(0,0,0,0.55)) */
    val dialogScrim: Color = Color(0x8C000000),
    /** accent 바탕 위 글자 */
    val onAccent: Color = Color(0xFFFFFFFF),
    /** 켜기·끄기 스위치 손잡이 (RegionDark #FFFFFF) */
    val switchKnob: Color = Color(0xFFFFFFFF),

    // ===== 지역 지도 (RegionDark mapC, dark) =====
    /** 지도 바다 */
    val mapSea: Color = Color(0xFF1B2433),
    /** 지도 땅 */
    val mapLand: Color = Color(0xFF1F2124),
    /** 해안선·섬 박스 선 */
    val mapLine: Color = Color(0xFFA9AFB8),
    /** 시·도·섬 박스 라벨 */
    val mapLabel: Color = Color(0xFF8E949D),
    /** 지도 위 제목 글자 */
    val mapText: Color = Color(0xFFFFFFFF),
    /** 지도 위 보조 글자 (rgba(255,255,255,0.7)) */
    val mapSub: Color = Color(0xB3FFFFFF),
    /** 선택 지역 테두리·지금 지역 링 */
    val mapSelected: Color = Color(0xFFFFFFFF),
    /** 지역 사이 경계 점선 (mapLine 35%, 해안선보다 확실히 약하게) */
    val mapBorder: Color = Color(0x59A9AFB8),
    /** 시·도 묶음 색 (RegionDark C). 방문 지역 채우기·범례·선택 카드 점. 키는 regions_display.json의 group. 서울은 경기 색 */
    val regionGroup: Map<String, Color> = mapOf(
        "seoul" to Color(0xFF7FA8F5), "gg" to Color(0xFF7FA8F5), "gw" to Color(0xFF6ED3AC),
        "cb" to Color(0xFFF0B45A), "cn" to Color(0xFFE3CC6E), "gb" to Color(0xFFB99BF0),
        "gn" to Color(0xFFF28BA9), "jb" to Color(0xFF6CC9D8), "jn" to Color(0xFFA2D47E),
        "jj" to Color(0xFFF4A07E),
    ),
)

val LocalTraceColors = staticCompositionLocalOf { TraceColors() }
