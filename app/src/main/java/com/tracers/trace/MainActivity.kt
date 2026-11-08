package com.tracers.trace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tracers.trace.ui.TraceNavHost
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 단일 Activity. 모든 화면은 Navigation Compose로 전환.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TraceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TraceNavHost()
                }
            }
        }
    }
}
