package com.tracers.trace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.theme.TraceTheme

class MainActivity : ComponentActivity() {

    private lateinit var logger: SignalLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logger = SignalLogger(this)
        logger.start()

        setContent {
            TraceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        Text("신호 로거", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "버추얼 콘솔에서 값을 바꾸면 여기와 Logcat(SIGNAL)에 표시됩니다.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(24.dp))
                        SignalLogger.ALL.forEach { name ->
                            Text(
                                "$name : ${logger.latest[name] ?: "대기 중"}",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        logger.stop()
        super.onDestroy()
    }
}