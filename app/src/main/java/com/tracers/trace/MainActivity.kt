package com.tracers.trace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.tracers.trace.data.permission.RuntimePermissions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tracers.trace.domain.resolveDarkTheme
import com.tracers.trace.ui.TraceNavHost
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 단일 Activity. 모든 화면은 Navigation Compose로 전환.
 */
class MainActivity : ComponentActivity() {

    private val app get() = application as TraceApplication

    /** 런타임 권한을 한 번에 요청. 결과와 상관없이 다시 확인해 수집 시작 여부를 정한다 */
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            app.refreshPermissions()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 앱 시작 시 한 번 (화면 회전 등 재생성 때는 다시 묻지 않는다)
        if (savedInstanceState == null) {
            val ask = RuntimePermissions.toRequest(this)
            if (ask.isNotEmpty()) permissionLauncher.launch(ask.toTypedArray())
        }

        setContent {
            // A-03: 해·달 스위치 선택은 시스템 테마가 바뀌기 전까지만, 바뀌면 지우고 시스템을 따른다
            val systemDark = isSystemInDarkTheme()
            val choice by app.themeSetting.choice.collectAsState()
            LaunchedEffect(systemDark) { app.themeSetting.onSystemTheme(systemDark) }
            TraceTheme(darkTheme = resolveDarkTheme(choice, systemDark)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TraceNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 설정 앱에서 권한을 허용하고 돌아온 경우
        app.refreshPermissions()
        // 날짜가 바뀌었을 수 있어 정비 주기 다시 계산 (D-01)
        app.maintenanceSource.refresh()
    }
}
