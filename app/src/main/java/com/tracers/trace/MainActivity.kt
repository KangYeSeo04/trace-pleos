package com.tracers.trace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.tracers.trace.data.permission.RuntimePermissions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
            // A-03: 해·달 스위치로 고른 적 있으면 그 선택, 없으면 시스템 설정
            val chosenDark by app.themeSetting.dark.collectAsState()
            TraceTheme(darkTheme = chosenDark ?: isSystemInDarkTheme()) {
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
