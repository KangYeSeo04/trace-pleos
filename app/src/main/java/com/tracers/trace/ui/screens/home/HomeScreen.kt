package com.tracers.trace.ui.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracers.trace.R
import com.tracers.trace.ui.components.StatusChip
import com.tracers.trace.ui.components.ThemeSwitch
import com.tracers.trace.ui.components.Tile
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.THEME_CROSSFADE_MS
import com.tracers.trace.ui.theme.TraceTheme

/**
 * S01(정차), S02(주행) 홈 화면.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigateToEfficiency: () -> Unit = {},
    onNavigateToMaintenance: () -> Unit = {},
    onNavigateToCoin: () -> Unit = {},
    onNavigateToRegion: () -> Unit = {},
    onNavigateToSpace: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panel)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 앱 바
        HomeAppBar(isDriving = uiState.isDriving, onToggleTheme = onToggleTheme)

        // 비행 무대 (디자인 정차 비율: 250 / 390 = 25 / 39)
        HeroStage(
            caption = uiState.caption,
            captionSub = uiState.captionSub,
            captionIconPath = uiState.captionIconPath,
            isDriving = uiState.isDriving,
            onClick = onNavigateToSpace,
            modifier = Modifier.weight(25f)
        )

        // 타일 그리드 2×2 (디자인 정차 비율: 390 / 250 = 39 / 25)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(39f)
        ) {
            TileGrid(
                efficiency = uiState.efficiency,
                maintenance = uiState.maintenance,
                coin = uiState.coin,
                region = uiState.region,
                isDriving = uiState.isDriving,
                onEfficiencyClick = onNavigateToEfficiency,
                onMaintenanceClick = onNavigateToMaintenance,
                onCoinClick = onNavigateToCoin,
                onRegionClick = onNavigateToRegion
            )
        }
    }
}

/**
 * 홈 앱 바: TRACE 로고 + 해/달 스위치 + 상태 칩
 */
@Composable
private fun HomeAppBar(
    isDriving: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    // 상태 칩 색상
    val chipBg = if (isDriving) colors.accentSoft else colors.goodSoft
    val chipColor = if (isDriving) colors.accent else colors.good
    val chipText = if (isDriving) "주행 중" else "정차 중"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // TRACE 로고
            TraceLogo()

            // TRACE 텍스트
            Text(
                text = "TRACE",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.02.sp,
                color = colors.t1
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 해/달 스위치 (A-03): 누르면 선택을 저장하고 그 테마를 따른다
            ThemeSwitch(
                isDark = TraceTheme.isDark,
                onToggle = onToggleTheme
            )
        }

        // 상태 칩
        StatusChip(
            text = chipText,
            backgroundColor = chipBg,
            contentColor = chipColor
        )
    }
}

/**
 * TRACE 로고 (그라디언트 배경 + 비행기 궤적 아이콘)
 */
@Composable
private fun TraceLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(TraceTheme.colors.logoBgTop, TraceTheme.colors.logoBgBottom),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(30f, 30f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = TraceIcons.TraceLogo,
            contentDescription = "TRACE",
            tint = Color.Unspecified, // 아이콘 자체에 색상이 정의됨
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 비행 무대 (밤하늘 사진 + 그라데이션 + 캡션)
 */
@Composable
private fun HeroStage(
    caption: String,
    captionSub: String,
    captionIconPath: String?,
    isDriving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TraceTheme.colors.heroBg)
            .clickable(enabled = !isDriving, role = Role.Button, onClick = onClick)
            .onGloballyPositioned { coords ->
                val heightDp = with(density) { coords.size.height.toDp() }
                Log.d("HomeLayout", "HeroStage height: $heightDp")
            }
    ) {
        // 하늘 사진 (다크 밤하늘 / 라이트 낮 하늘). 테마가 바뀌면 1초 크로스페이드
        Crossfade(
            targetState = TraceTheme.isDark,
            animationSpec = tween(durationMillis = THEME_CROSSFADE_MS),
            label = "heroSky",
        ) { dark ->
            Image(
                painter = painterResource(id = if (dark) R.drawable.sky_night_hero else R.drawable.sky_day_hero),
                contentDescription = if (dark) "밤하늘" else "낮 하늘",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 하단 그라데이션
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        // 캡션
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // 아이콘 (있을 때만)
                if (captionIconPath != null) {
                    Icon(
                        imageVector = TraceIcons.Bolt, // captionIconPath에 따라 달라야 하지만, 일단 Bolt 사용
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    text = caption,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = captionSub,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

/**
 * 타일 그리드 2×2 (고정 높이 390dp)
 */
@Composable
private fun TileGrid(
    efficiency: TileState,
    maintenance: TileState,
    coin: TileState,
    region: TileState,
    isDriving: Boolean,
    onEfficiencyClick: () -> Unit,
    onMaintenanceClick: () -> Unit,
    onCoinClick: () -> Unit,
    onRegionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // 주행 모드일 때는 상세 이동 대신 음성 안내 (TODO: A-04)
    // TODO(A-04): TTS 호출
    val effClick: () -> Unit = if (isDriving) { {} } else onEfficiencyClick
    val maintClick: () -> Unit = if (isDriving) { {} } else onMaintenanceClick
    val coinClick: () -> Unit = if (isDriving) { {} } else onCoinClick
    val regionClick: () -> Unit = if (isDriving) { {} } else onRegionClick

    Column(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val heightDp = with(density) { coords.size.height.toDp() }
                Log.d("HomeLayout", "TileGrid height: $heightDp (mode: ${if (isDriving) "driving" else "parked"})")
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Tile(
                state = efficiency,
                isDriving = isDriving,
                onClick = effClick,
                modifier = Modifier.weight(1f)
            )
            Tile(
                state = maintenance,
                isDriving = isDriving,
                onClick = maintClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Tile(
                state = coin,
                isDriving = isDriving,
                onClick = coinClick,
                modifier = Modifier.weight(1f)
            )
            Tile(
                state = region,
                isDriving = isDriving,
                onClick = regionClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================
// Preview
// ============================================================

@Preview(widthDp = 600, heightDp = 800)
@Composable
private fun HomeScreenParkedPreview() {
    TraceTheme {
        HomeScreen(uiState = SampleHomeUiState.parked)
    }
}

@Preview(widthDp = 600, heightDp = 800)
@Composable
private fun HomeScreenDrivingPreview() {
    TraceTheme {
        HomeScreen(uiState = SampleHomeUiState.driving)
    }
}

/**
 * Preview용 샘플 데이터.
 * 디자인의 예시 숫자를 그대로 사용.
 */
private object SampleHomeUiState {
    val parked = HomeUiState(
        isDriving = false,
        caption = "충전은 목요일까지 여유 있어요",
        captionSub = "배터리 64% · 하루 평균 34 km 기준",
        captionIconPath = "M13 3L5 14h6l-1 7 8-11h-6z", // 번개
        efficiency = TileState.Efficiency(
            value = "6.4",
            unit = "km/kWh",
            status = "평균보다 1.1 좋아요",
            statusLevel = StatusLevel.Good,
            barPercent = 78f,
            subText = "주행 78% · 공조 17% · 회생 0.9 kWh"
        ),
        maintenance = TileState.Maintenance(
            value = "12",
            unit = "일",
            status = "와이퍼 블레이드 교체",
            statusLevel = StatusLevel.Warn,
            barPercent = 97f,
            subText = "다음 · 에어컨 필터 1,200 km"
        ),
        coin = TileState.Coin(
            value = "1,284",
            unit = "",
            status = "이번 주행 +37",
            statusLevel = StatusLevel.Gold,
            barPercent = 63f,
            barMarkPercent = 50f,
            subText = "안전점수 85.3 · 적립 기준 80"
        ),
        region = TileState.Region(
            city = "서울",
            district = "중랑구",
            mapCode = "11000",
            mapLatitude = 37.6063,
            mapLongitude = 127.0928,
            value = "서울",
            unit = "중랑구",
            status = "161곳 중 12곳 방문",
            statusLevel = StatusLevel.Normal,
            barPercent = 7f,
            subText = "달린 거리 9,840 km"
        )
    )

    val driving = HomeUiState(
        isDriving = true,
        caption = "매끄럽게 비행 중",
        captionSub = "오늘 42.6 km · 37 km 깨끗하게",
        captionIconPath = null,
        efficiency = TileState.Efficiency(
            value = "6.4",
            unit = "km/kWh",
            status = "평소보다 좋아요",
            statusLevel = StatusLevel.Good,
            barPercent = 78f,
            subText = ""
        ),
        maintenance = TileState.Maintenance(
            value = "12",
            unit = "일",
            status = "와이퍼 블레이드 교체",
            statusLevel = StatusLevel.Warn,
            barPercent = 97f,
            subText = ""
        ),
        coin = TileState.Coin(
            value = "1,284",
            unit = "",
            status = "별 모으는 중",
            statusLevel = StatusLevel.Gold,
            barPercent = 63f,
            subText = ""
        ),
        region = TileState.Region(
            city = "서울",
            district = "성동구",
            mapCode = "11000",
            mapLatitude = 37.5634,
            mapLongitude = 127.0369,
            value = "서울",
            unit = "성동구",
            status = "성동구",
            statusLevel = StatusLevel.Normal,
            barPercent = 7f,
            subText = ""
        )
    )
}
