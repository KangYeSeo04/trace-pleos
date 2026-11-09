package com.tracers.trace.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * TRACE 색상 토큰. 기본값은 다크 묶음(CLAUDE.md "다크 테마 토큰" 표).
 * 라이트 묶음은 [LightTraceColors] (design/LIGHT.md 표 + Light HTML). 화면 코드는 토큰만 쓰고
 * TraceTheme이 테마에 따라 묶음을 바꿔 준다.
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
    /** 충전 예측 막대 (EffDark/Light T.bar) */
    val bar: Color = Color(0xFF363A41),
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
    /** 해·달 스위치 손잡이 (Home knobBg) */
    val themeKnob: Color = Color(0xFF3A3F48),
    /** 해·달 스위치 손잡이 아이콘 (Home knobFg) */
    val themeKnobIcon: Color = Color(0xFFE8ECF2),
    /** 하늘 탭 비행 영상 뒤 바탕 (SpaceDark #0A0B0D / SpaceLight #1B3566) */
    val spaceBg: Color = Color(0xFF0A0B0D),

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

/** 다크 묶음 */
val DarkTraceColors = TraceColors()

/**
 * 라이트 묶음. 화면마다 조금씩 다른 HTML 값은 상세 화면(Eff·Coin Light T) 기준으로 통일했다.
 * 홈 HTML과 다른 값: caution #C97A08→#D9820B, goodSoft #E5F4EC→#E3F3EB, gold #C08414→#D19A1E.
 */
val LightTraceColors = TraceColors(
    panel = Color(0xFFFFFFFF),
    tile = Color(0xFFF3F4F6),
    t1 = Color(0xFF111418),
    t2 = Color(0xFF5C636D),
    t3 = Color(0xFF8A9099),
    line = Color(0xFFE5E7EB),
    track = Color(0xFFE1E4E8),           // HomeLight TOK.track
    bar = Color(0xFFDADDE2),             // EffLight bar
    itemTrack = Color(0xFFDFE2E6),       // MaintLight track
    barNeutral = Color(0xFF9AA1AA),      // MaintLight ok
    accent = Color(0xFF1677E6),
    accentSoft = Color(0xFFE7F0FC),      // HomeLight 칩
    accentCard = Color(0xFFEBF3FE),      // MaintLight accentSoft
    good = Color(0xFF12935F),
    goodSoft = Color(0xFFE3F3EB),        // Eff·CoinLight
    caution = Color(0xFFD9820B),         // Eff·CoinLight
    cautionSoft = Color(0x24D9820B),     // HTML에 없음. 다크와 같은 0.14 비율
    warn = Color(0xFFD9474A),
    gold = Color(0xFFD19A1E),            // CoinLight
    goldSoft = Color(0xFFFFF4DC),        // CoinLight cellOn
    cellOff = Color(0xFFE9EBEE),         // CoinLight
    dim = Color(0xFFB7BCC3),             // CoinLight
    heroBg = Color(0xFF1F3B6A),          // HomeLight·CoinLight heroBg
    themeKnob = Color(0xFFFFFFFF),
    themeKnobIcon = Color(0xFFF2A93B),
    spaceBg = Color(0xFF1B3566),
    // RegionLight mapC
    mapSea = Color(0xFFDCE5EF),
    mapLand = Color(0xFFFFFFFF),
    mapLine = Color(0xFFA3ACB8),
    mapLabel = Color(0xFF7C838D),
    mapText = Color(0xFF111418),
    mapSub = Color(0xFF5C636D),
    // RegionLight 선택 지역 stroke (theme === 'dark' ? '#FFFFFF' : '#111418')
    mapSelected = Color(0xFF111418),
    mapBorder = Color(0x59A3ACB8),
)

/** 테마 전환 크로스페이드용. t=0이면 a, t=1이면 b */
fun lerpTraceColors(a: TraceColors, b: TraceColors, t: Float): TraceColors = when (t) {
    0f -> a
    1f -> b
    else -> TraceColors(
        panel = lerp(a.panel, b.panel, t),
        tile = lerp(a.tile, b.tile, t),
        t1 = lerp(a.t1, b.t1, t),
        t2 = lerp(a.t2, b.t2, t),
        t3 = lerp(a.t3, b.t3, t),
        line = lerp(a.line, b.line, t),
        track = lerp(a.track, b.track, t),
        bar = lerp(a.bar, b.bar, t),
        itemTrack = lerp(a.itemTrack, b.itemTrack, t),
        barNeutral = lerp(a.barNeutral, b.barNeutral, t),
        accent = lerp(a.accent, b.accent, t),
        accentSoft = lerp(a.accentSoft, b.accentSoft, t),
        accentCard = lerp(a.accentCard, b.accentCard, t),
        good = lerp(a.good, b.good, t),
        goodSoft = lerp(a.goodSoft, b.goodSoft, t),
        caution = lerp(a.caution, b.caution, t),
        cautionSoft = lerp(a.cautionSoft, b.cautionSoft, t),
        warn = lerp(a.warn, b.warn, t),
        gold = lerp(a.gold, b.gold, t),
        goldSoft = lerp(a.goldSoft, b.goldSoft, t),
        cellOff = lerp(a.cellOff, b.cellOff, t),
        dim = lerp(a.dim, b.dim, t),
        heroBg = lerp(a.heroBg, b.heroBg, t),
        heroTrail = lerp(a.heroTrail, b.heroTrail, t),
        logoBgTop = lerp(a.logoBgTop, b.logoBgTop, t),
        logoBgBottom = lerp(a.logoBgBottom, b.logoBgBottom, t),
        heroText = lerp(a.heroText, b.heroText, t),
        heroScrim = lerp(a.heroScrim, b.heroScrim, t),
        heroButton = lerp(a.heroButton, b.heroButton, t),
        heroButtonText = lerp(a.heroButtonText, b.heroButtonText, t),
        dialogScrim = lerp(a.dialogScrim, b.dialogScrim, t),
        onAccent = lerp(a.onAccent, b.onAccent, t),
        switchKnob = lerp(a.switchKnob, b.switchKnob, t),
        themeKnob = lerp(a.themeKnob, b.themeKnob, t),
        themeKnobIcon = lerp(a.themeKnobIcon, b.themeKnobIcon, t),
        spaceBg = lerp(a.spaceBg, b.spaceBg, t),
        mapSea = lerp(a.mapSea, b.mapSea, t),
        mapLand = lerp(a.mapLand, b.mapLand, t),
        mapLine = lerp(a.mapLine, b.mapLine, t),
        mapLabel = lerp(a.mapLabel, b.mapLabel, t),
        mapText = lerp(a.mapText, b.mapText, t),
        mapSub = lerp(a.mapSub, b.mapSub, t),
        mapSelected = lerp(a.mapSelected, b.mapSelected, t),
        mapBorder = lerp(a.mapBorder, b.mapBorder, t),
        regionGroup = a.regionGroup.mapValues { (k, c) -> lerp(c, b.regionGroup[k] ?: c, t) },
    )
}

val LocalTraceColors = staticCompositionLocalOf { DarkTraceColors }
