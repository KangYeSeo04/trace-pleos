package com.tracers.trace.ui.screens.coin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 코인 상세 화면 (S06). 디자인: design/html/CoinDark.dc.html (600 × 1900)
 *
 * @param onNavigateBack 뒤로 → 홈
 * @param onClaimCoins "N코인 받기" 버튼. 적립 처리는 2단계(E-04)
 * @param onOpenHelp 헤더 "적립 기준 보기". 안내 창은 2회차
 */
@Composable
fun CoinScreen(
    uiState: CoinUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onClaimCoins: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
) {
    val colors = TraceTheme.colors
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    // 히어로가 스크롤로 화면 밖에 나가면 애니메이션을 멈춘다
    val heroVisible by remember(density) {
        derivedStateOf { scrollState.value < with(density) { (HeroTop + HeroHeight).toPx() } }
    }

    Column(
        modifier = modifier
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
                    onClick = onOpenHelp
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
                onClaimCoins = onClaimCoins
            )

            SectionTitle("안전점수")
            SafetyScoreCard(chart = uiState.safetyScore)

            // 아래 카드는 2회차. 디자인 순서와 간격만 자리 잡아 둔다.
            // 이번 주행 1km 기록: SectionTitle("이번 주행 1km 기록") + 카드 (위 26)
            TripKmRecordSection(record = uiState.tripRecord)
            // 상점 바로가기: 72 높이 줄 (위 10)
            ShopLinkSection(shop = uiState.shop)
            // 받은 기록: SectionTitle("받은 기록") + 목록 (위 26)
            CoinHistorySection(history = uiState.history)
        }
    }
}

/** 히어로 높이 (디자인 240) */
private val HeroHeight = 240.dp
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
    onClaimCoins: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.heroBg)
    ) {
        Image(
            painter = painterResource(R.drawable.coin_bg_night),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // object-position 50% 40% → 세로 바이어스 0.4 × 2 − 1
            alignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.2f),
            modifier = Modifier.fillMaxSize()
        )
        CoinHeroCanvas(animate = animate, modifier = Modifier.fillMaxSize())

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
                    style = type.number(44f).copy(lineHeight = 44.sp),
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
    // TODO(2회차): 37칸 격자(10열, 칸 34, 간격 6), 급가속·급감속·과속·차량 경고 4칸, 제외 안내 문구
}

/** 상점 바로가기 줄 → S07. */
@Composable
private fun ShopLinkSection(shop: ShopTeaser?) {
    // TODO(2회차): 40×40 아이콘 + "상점" + 보관함·목표 문구 + 화살표, 탭 → 상점
}

/** 받은 기록 (E-04). */
@Composable
private fun CoinHistorySection(history: List<CoinHistoryEntry>) {
    // TODO(2회차): 받은 +N(t1)·쓴 −N(t2) 목록, 줄 사이 1px line. 헤더 "적립 기준" 안내 창도 2회차
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

/** 1,284 형식 */
internal fun formatCoins(value: Int): String = String.format(Locale.KOREA, "%,d", value)

// ===== Preview =====

@Preview(widthDp = 600, heightDp = 1900, showBackground = true)
@Composable
private fun CoinScreenPreview() {
    TraceTheme { CoinScreen(uiState = SampleData.coinUiState, onNavigateBack = {}) }
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
