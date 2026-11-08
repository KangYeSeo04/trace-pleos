package com.tracers.trace.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tracers.trace.ui.screens.HomeScreen

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
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(TraceDestinations.HOME) {
            HomeScreen(
                onNavigateToEfficiency = { navController.navigate(TraceDestinations.EFFICIENCY) },
                onNavigateToMaintenance = { navController.navigate(TraceDestinations.MAINTENANCE) },
                onNavigateToCoin = { navController.navigate(TraceDestinations.COIN) },
                onNavigateToRegion = { navController.navigate(TraceDestinations.REGION) },
                onNavigateToSpace = { navController.navigate(TraceDestinations.SPACE) }
            )
        }
        // TODO: 1단계에서 각 화면 추가
    }
}
