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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tracers.trace.data.sample.SampleData
import com.tracers.trace.data.uxrestrictions.CarUxRestrictionsMonitor
import com.tracers.trace.data.vehicle.PleosVehicleSignals
import com.tracers.trace.domain.DriveState
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.screens.efficiency.EfficiencyScreen
import com.tracers.trace.ui.screens.home.HomeScreen
import com.tracers.trace.ui.screens.home.HomeViewModel
import com.tracers.trace.ui.screens.spaceflight.SpaceFlightScreen
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
    val context = LocalContext.current

    // HomeViewModel 생성 (Activity 생명주기 동안 유지)
    val homeViewModel = remember {
        val vehicleSignals = PleosVehicleSignals(context)
        HomeViewModel(vehicleSignals)
    }

    // CarUxRestrictionsMonitor 생성 (Activity 생명주기 동안 유지)
    val uxRestrictionsMonitor = remember {
        CarUxRestrictionsMonitor(context).apply { start() }
    }

    // A-02: 주행 시작 시 자동 복귀
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val driveState by homeViewModel.driveState.collectAsState()
    val requiresDistractionOptimization by uxRestrictionsMonitor.requiresDistractionOptimization.collectAsState()

    // 시스템 제한의 이전 값 추적 (false→true 전환 감지용)
    var previousUxRestriction by remember { mutableStateOf(requiresDistractionOptimization) }

    LaunchedEffect(driveState, requiresDistractionOptimization, currentRoute) {
        // 자동 복귀 조건:
        // 1. DriveStateDetector가 DRIVING 상태이거나
        // 2. 시스템 제한이 false → true로 바뀌는 순간 (이미 true인 상태로는 복귀하지 않음)
        val isDriving = driveState == DriveState.DRIVING
        val uxRestrictionJustActivated = !previousUxRestriction && requiresDistractionOptimization

        if ((isDriving || uxRestrictionJustActivated) && currentRoute != TraceDestinations.HOME) {
            navController.popBackStack(TraceDestinations.HOME, inclusive = false)
        }

        // 이전 값 업데이트
        previousUxRestriction = requiresDistractionOptimization
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(TraceDestinations.HOME) {
            val homeUiState = homeViewModel.uiState.collectAsState()

            HomeScreen(
                uiState = homeUiState.value,
                onNavigateToEfficiency = { navController.navigate(TraceDestinations.EFFICIENCY) },
                onNavigateToMaintenance = { navController.navigate(TraceDestinations.MAINTENANCE) },
                onNavigateToCoin = { navController.navigate(TraceDestinations.COIN) },
                onNavigateToRegion = { navController.navigate(TraceDestinations.REGION) },
                onNavigateToSpace = { navController.navigate(TraceDestinations.SPACE) }
            )
        }
        composable(TraceDestinations.EFFICIENCY) {
            // TODO(feat): ViewModel 연결
            EfficiencyScreen(
                uiState = SampleData.efficiencyUiState,
                onNavigateBack = { navController.popBackStack() }
            )
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
            SpaceFlightScreen(
                onNavigateBack = { navController.popBackStack() },
                isDrivingRestricted = driveState == DriveState.DRIVING
            )
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
