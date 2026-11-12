package com.tracers.trace.ui.screens.coin

import com.tracers.trace.ui.theme.THEME_CROSSFADE_MS
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.tracers.trace.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tracers.trace.data.sample.SampleData
import com.tracers.trace.ui.components.DetailHeader
import com.tracers.trace.ui.components.HeaderIconButton
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.components.TwoColumns
import com.tracers.trace.ui.theme.TraceLayout
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 코인 상세 화면 (S06). 디자인: design/html/CoinDark.dc.html (600 × 1900)
 *
 * @param onNavigateBack 뒤로 → 홈
 * @param onClaimCoins "N코인 받기" 버튼. 적립 처리는 2단계(E-04)
 * @param onOpenShop 꾸미기 진입 카드 → 상점(S07)
 */
@Composable
fun CoinScreen(
    uiState: CoinUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onClaimCoins: () -> Unit = {},
    onOpenShop: () -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
) {
    val colors = TraceTheme.colors
    val density = LocalDensity.current
    val wide = TraceTheme.layout == TraceLayout.Wide
    val heroHeight = if (wide) HeroHeightWide else HeroHeight
    // 히어로가 스크롤로 화면 밖에 나가면 애니메이션을 멈춘다
    val heroVisible by remember(density, heroHeight) {
        derivedStateOf { scrollState.value < with(density) { (HeroTop + heroHeight).toPx() } }
    }
    // 헤더 "적립 기준 보기" 안내 창
    var helpOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.panel)
    ) {
        // 헤더 (sticky): 높이 52 + 아래 1px 구분선
        DetailHeader(
            title = "코인",
            onBack = onNavigateBack,
            actions = {
                HeaderIconButton(
                    icon = TraceIcons.Help,
                    contentDescription = "적립 기준 보기",
                    onClick = { helpOpen = true }
                )
            }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line)
        )

        // 스크롤 영역: 좌우 20, 아래 28
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = HeroTop, bottom = 28.dp)
        ) {
            CoinHero(
                uiState = uiState,
                animate = heroVisible,
                wide = wide,
                onClaimCoins = onClaimCoins
            )

            if (wide) {
                // FullCoin: 히어로 아래 4, 2열 grid(간격 24) — 왼쪽 안전점수·1km 기록, 오른쪽 상점·받은 기록
                Spacer(modifier = Modifier.height(4.dp))
                TwoColumns(
                    left = {
                        SectionTitle("안전점수")
                        SafetyScoreCard(chart = uiState.safetyScore)
                        TripKmRecordSection(record = uiState.tripRecord)
                    },
                    right = {
                        ShopLinkSection(shop = uiState.shop, onOpenShop = onOpenShop)
                        CoinHistorySection(history = uiState.history)
                    },
                )
            } else {
                SectionTitle("안전점수")
                SafetyScoreCard(chart = uiState.safetyScore)

                TripKmRecordSection(record = uiState.tripRecord)
                ShopLinkSection(shop = uiState.shop, onOpenShop = onOpenShop)
                CoinHistorySection(history = uiState.history)
            }
        }
    }

    if (helpOpen) {
        AccrualRulesDialog(onClose = { helpOpen = false })
    }
    }
}

/** 히어로 높이 (디자인 240) */
private val HeroHeight = 240.dp
/** 전체 화면 히어로 높이 (FullCoin 340) */
private val HeroHeightWide = 340.dp
/** 스크롤 영역 위 여백 (디자인 margin-top 12) */
private val HeroTop = 12.dp

/**
 * 상단 히어로: 밤하늘 사진 + 비행운·기체 캔버스.
 * 디자인: 높이 240, 모서리 20, 사진 object-fit cover / object-position 50% 40%.
 */
@Composable
private fun CoinHero(
    uiState: CoinUiState,
    animate: Boolean,
    wide: Boolean,
    onClaimCoins: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (wide) HeroHeightWide else HeroHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.heroBg)
    ) {
        // 다크는 밤하늘 사진 + 캔버스 애니메이션, 라이트는 낮 하늘 정지 사진 (CoinLight isLight 분기)
        Crossfade(
            targetState = TraceTheme.isDark,
            animationSpec = tween(durationMillis = THEME_CROSSFADE_MS),
            label = "coinHero",
        ) { dark ->
            Box(Modifier.fillMaxSize()) {
                if (wide) {
                    // 전체 화면은 라이트·다크 모두 넓은 정지 사진 (FULL.md: 다크도 canvas 없음)
                    Image(
                        painter = painterResource(if (dark) R.drawable.sky_night_hero_wide else R.drawable.sky_day_hero_wide),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (dark) {
                    Image(
                        painter = painterResource(R.drawable.coin_bg_night),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        // object-position 50% 40% → 세로 바이어스 0.4 × 2 − 1
                        alignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.2f),
                        modifier = Modifier.fillMaxSize()
                    )
                    CoinHeroCanvas(animate = animate, modifier = Modifier.fillMaxSize())
                } else {
                    Image(
                        painter = painterResource(R.drawable.sky_day_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 하단 150 그라데이션 (투명 → 0.6)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.heroScrim)))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, end = 22.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "보유 코인",
                    style = type.body(14f),
                    color = colors.heroText.copy(alpha = 0.72f)
                )
                Text(
                    text = formatCoins(uiState.balance),
                    style = type.numberTight(44f),
                    color = colors.heroText
                )
                Text(
                    text = uiState.starsThisTrip
                        ?.let { "이번 주행에서 별 ${it}개를 모았어요" }
                        ?: "주행하면 1 km마다 별이 생겨요",
                    style = type.body(14f),
                    color = colors.heroText.copy(alpha = 0.8f)
                )
            }

            // E-04: 받을 코인이 있을 때만
            if (uiState.claimableCoins > 0) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.heroButton)
                        .clickable(role = Role.Button, onClick = onClaimCoins)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${formatCoins(uiState.claimableCoins)}코인 받기",
                        style = type.body(15f, FontWeight.Bold),
                        color = colors.heroButtonText
                    )
                }
            }
        }
    }
}

/** 이번 주행 1km 기록 (E-03). */
@Composable
private fun TripKmRecordSection(record: TripKmRecord?) {
    SectionTitle("이번 주행 1km 기록")
    TripKmRecordCard(record = record)
}

/**
 * 꾸미기 진입 카드 → S07. 디자인: 위 10, 높이 72, 모서리 20, 좌우 20, 간격 14, 아이콘 칸 40·모서리 12.
 */
@Composable
private fun ShopLinkSection(shop: ShopTeaser?, onOpenShop: () -> Unit) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .clickable(role = Role.Button, onClickLabel = "상점 열기", onClick = onOpenShop)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(colors.logoBgTop, colors.logoBgBottom))),
            contentAlignment = Alignment.Center
        ) {
            Image(imageVector = TraceIcons.ShopTrail, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "상점", style = type.body(16f, FontWeight.SemiBold), color = colors.t1)
            Text(
                text = shop?.let { "${it.nextItemName}까지 ${formatCoins(it.coinsToNext)}코인" } ?: "꾸미기 아이템 둘러보기",
                style = type.body(13f),
                color = colors.t2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = TraceIcons.ChevronRight,
            contentDescription = null,
            tint = colors.t3,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 받은 기록 (E-04). 최근 순, 최대 30건. 줄 안쪽 15 20, 줄 사이 1px line.
 */
@Composable
private fun CoinHistorySection(history: List<CoinHistoryEntry>) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    SectionTitle("받은 기록")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
    ) {
        if (history.isEmpty()) {
            Text(
                text = "아직 받은 코인이 없어요",
                style = type.body(15f),
                color = colors.t2,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
            )
            return@Column
        }
        history.take(CoinUiState.HISTORY_LIMIT).forEachIndexed { i, entry ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(text = entry.title, style = type.body(15f, FontWeight.SemiBold), color = colors.t1)
                    Text(text = entry.subtitle, style = type.body(13f), color = colors.t2)
                }
                Text(
                    text = formatSignedCoins(entry.amount),
                    style = type.number(16f),
                    color = if (entry.amount < 0) colors.t2 else colors.t1
                )
            }
        }
    }
}

/** 섹션 제목: 위 26, 좌우 4, 아래 10, 15sp 600 t2 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = TraceTheme.type.body(15f, FontWeight.SemiBold),
        color = TraceTheme.colors.t2,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 26.dp, bottom = 10.dp)
    )
}

/** 적립 "+31", 구매 "−500" (디자인은 U+2212 빼기 기호) */
internal fun formatSignedCoins(value: Int): String =
    if (value < 0) "\u2212" + formatCoins(-value) else "+" + formatCoins(value)

/** 1,284 형식 */
internal fun formatCoins(value: Int): String = String.format(Locale.KOREA, "%,d", value)

// ===== Preview =====

@Preview(name = "다크", widthDp = 600, heightDp = 1900, showBackground = true)
@Composable
private fun CoinScreenDarkPreview() {
    TraceTheme(darkTheme = true) { CoinScreen(uiState = SampleData.coinUiState, onNavigateBack = {}) }
}

@Preview(name = "라이트", widthDp = 600, heightDp = 1900, showBackground = true)
@Composable
private fun CoinScreenLightPreview() {
    TraceTheme(darkTheme = false) { CoinScreen(uiState = SampleData.coinUiState, onNavigateBack = {}) }
}

@Preview(name = "전체 화면 · 다크", widthDp = 1200, heightDp = 800)
@Composable
private fun CoinScreenWideDarkPreview() {
    TraceTheme(darkTheme = true) { CoinScreen(uiState = SampleData.coinUiState, onNavigateBack = {}) }
}

@Preview(name = "전체 화면 · 라이트", widthDp = 1200, heightDp = 800)
@Composable
private fun CoinScreenWideLightPreview() {
    TraceTheme(darkTheme = false) { CoinScreen(uiState = SampleData.coinUiState, onNavigateBack = {}) }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun CoinScreenStoppedPreview() {
    TraceTheme { CoinScreen(uiState = SampleData.coinUiStateStopped, onNavigateBack = {}) }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun CoinScreenEmptyPreview() {
    TraceTheme { CoinScreen(uiState = SampleData.coinUiStateEmpty, onNavigateBack = {}) }
}
