package com.tracers.trace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.screens.home.HomeScreen
import com.tracers.trace.ui.screens.home.HomeUiState
import com.tracers.trace.ui.screens.home.StatusLevel
import com.tracers.trace.ui.screens.home.TileState
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 화면 경로. 기획안 ID와 대응:
 * - home: S01(정차), S02(주행)
 * - efficiency: S03 전비 상세
 * - maintenance: S04 정비 상세, S05 기록 추가
 * - coin: S06 코인 상세
 * - shop: S07 상점·꾸미기
 * - region: S08 지역 상세
 * - space: 우주 비행 (신규)
 * - universe: 내 우주 (신규)
 */
object TraceDestinations {
    const val HOME = "home"
    const val EFFICIENCY = "efficiency"
    const val MAINTENANCE = "maintenance"
    const val COIN = "coin"
    const val SHOP = "shop"
    const val REGION = "region"
    const val SPACE = "space"
    const val UNIVERSE = "universe"
}

@Composable
fun TraceNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = TraceDestinations.HOME
) {
    // 샘플 상태 (2단계에서 ViewModel로 교체)
    val homeUiState = remember { createSampleHomeUiState() }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(TraceDestinations.HOME) {
            HomeScreen(
                uiState = homeUiState,
                onNavigateToEfficiency = { navController.navigate(TraceDestinations.EFFICIENCY) },
                onNavigateToMaintenance = { navController.navigate(TraceDestinations.MAINTENANCE) },
                onNavigateToCoin = { navController.navigate(TraceDestinations.COIN) },
                onNavigateToRegion = { navController.navigate(TraceDestinations.REGION) },
                onNavigateToSpace = { navController.navigate(TraceDestinations.SPACE) }
            )
        }
        composable(TraceDestinations.EFFICIENCY) {
            PlaceholderScreen(onBack = { navController.popBackStack() })
        }
        composable(TraceDestinations.MAINTENANCE) {
            PlaceholderScreen(onBack = { navController.popBackStack() })
        }
        composable(TraceDestinations.COIN) {
            PlaceholderScreen(onBack = { navController.popBackStack() })
        }
        composable(TraceDestinations.REGION) {
            PlaceholderScreen(onBack = { navController.popBackStack() })
        }
        composable(TraceDestinations.SPACE) {
            PlaceholderScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * 빈 화면 + 뒤로 버튼 (상세 화면 자리 표시용).
 */
@Composable
private fun PlaceholderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panel)
    ) {
        // 뒤로 버튼
        Box(
            modifier = Modifier
                .padding(20.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.tile)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TraceIcons.ArrowBack,
                contentDescription = "뒤로 가기",
                tint = colors.t1,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 샘플 홈 상태 (2단계에서 ViewModel로 교체).
 */
private fun createSampleHomeUiState() = HomeUiState(
    isDriving = false,
    caption = "충전은 목요일까지 여유 있어요",
    captionSub = "배터리 64% · 하루 평균 34 km 기준",
    captionIconPath = "M13 3L5 14h6l-1 7 8-11h-6z",
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
        locationX = 72.9f,
        locationY = 28.6f,
        value = "서울",
        unit = "중랑구",
        status = "161곳 중 12곳 방문",
        statusLevel = StatusLevel.Normal,
        barPercent = 7f,
        subText = "달린 거리 9,840 km"
    )
)
