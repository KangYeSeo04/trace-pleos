package com.tracers.trace.probe

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracers.trace.ui.theme.TraceTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 사자 카트용 차량 데이터 확인 화면 (PROBE용, 출시 전 정리).
 * adb shell am start --user 10 -n com.tracers.trace/.probe.ProbeActivity
 */
class ProbeActivity : ComponentActivity() {

    private val board = ProbeBoard()
    private lateinit var aaos: AaosProbe
    private lateinit var vehicle: VehicleProbe
    private lateinit var services: ServiceProbe
    private lateinit var sensors: SensorProbe
    private var started = false
    private var chip: View? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            result.forEach { (p, ok) -> board.value(permItem(p), if (ok) "허용" else "거부") }
            startProbes()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aaos = AaosProbe(this, board)
        vehicle = VehicleProbe(this, board)
        services = ServiceProbe(this, board)
        sensors = SensorProbe(this, board)

        // 매니페스트에 선언한 권한 중 아직 없는 것만 요청 (일반 권한은 이미 허용돼 있음)
        val declared = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissionsOrEmpty()
        declared.forEach { p ->
            if (checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED)
                board.value(permItem(p), "허용")
        }
        val missing = declared.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startProbes() else permissionLauncher.launch(missing.toTypedArray())

        setContent {
            TraceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("PROBE · 차량 데이터 확인", style = MaterialTheme.typography.headlineMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                            Button(onClick = ::readAgain) { Text("다시 읽기") }
                            Button(onClick = ::showChip) { Text("F 오버레이 칩 띄우기 (오른쪽 위)") }
                            Button(onClick = ::removeChip) { Text("칩 제거") }
                            Button(onClick = { services.requestTestRoute() }) { Text("기획1 목적지 시험") }
                            Button(onClick = { services.cancelTestRoute() }) { Text("경로 취소") }
                        }
                        ProbeTable()
                    }
                }
            }
        }
    }

    /** pleos.car.permission.CAR_ENERGY / android.car.permission.CAR_ENERGY 처럼 끝 이름이 같은 것을 구분 */
    private fun permItem(p: String) = "권한·${p.substringBefore(".permission")}.${p.substringAfterLast('.')}"

    private fun PackageInfo.requestedPermissionsOrEmpty(): List<String> =
        requestedPermissions?.toList().orEmpty()

    private fun startProbes() {
        if (started) return
        started = true
        overlayStatus()
        aaos.start()
        vehicle.start()
        services.start()
        sensors.start()
    }

    private fun readAgain() {
        overlayStatus()
        aaos.readAgain()
        vehicle.readAgain()
        services.readAgain()
        sensors.readAgain()
    }

    // ---------- F. 오버레이 ----------

    private fun overlayStatus() = board.value(OVERLAY, "canDrawOverlays=${Settings.canDrawOverlays(this)}")

    private fun showChip() {
        overlayStatus()
        if (chip != null) return
        if (!Settings.canDrawOverlays(this)) {
            board.error(CHIP, "권한 없음 (appops SYSTEM_ALERT_WINDOW)"); return
        }
        val size = (64 * resources.displayMetrics.density).toInt()
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            title = "ProbeChip"
        }
        val v = TextView(applicationContext).apply {
            text = "PROBE"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(255, 140, 0))
            }
        }
        try {
            getSystemService(WindowManager::class.java).addView(v, lp)
            chip = v
            v.post {
                val loc = IntArray(2).also { v.getLocationOnScreen(it) }
                board.value(CHIP, "addView 성공 ${v.width}x${v.height}px loc=${loc[0]},${loc[1]} (실제 좌표는 dumpsys window)")
            }
        } catch (t: Throwable) {
            board.error(CHIP, t)
        }
    }

    private fun removeChip() {
        chip?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        if (chip != null) board.value(CHIP, "제거됨")
        chip = null
    }

    override fun onDestroy() {
        removeChip()
        aaos.stop()
        vehicle.stop()
        services.stop()
        sensors.stop()
        super.onDestroy()
    }

    // ---------- 표 ----------

    @androidx.compose.runtime.Composable
    private fun ProbeTable() {
        val header = MaterialTheme.colorScheme.surfaceVariant
        Row(Modifier.fillMaxWidth().background(header).padding(6.dp)) {
            Cell("항목", 3f, bold = true)
            Cell("지원 여부", 3f, bold = true)
            Cell("현재 값", 4f, bold = true)
            Cell("갱신 시각", 1.3f, bold = true)
            Cell("간격(ms)", 0.9f, bold = true)
            Cell("오류", 3f, bold = true)
        }
        LazyColumn {
            items(board.order.toList(), key = { it }) { item ->
                val r = board.rows[item] ?: ProbeBoard.Row()
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Cell(item, 3f)
                    Cell(r.support, 3f)
                    Cell(r.value, 4f)
                    Cell(if (r.updatedAt == 0L) "-" else timeFmt.format(Date(r.updatedAt)), 1.3f)
                    Cell(r.gapMs?.toString() ?: "-", 0.9f)
                    Cell(r.error, 3f, error = r.error.isNotEmpty())
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun RowScope.Cell(text: String, weight: Float, bold: Boolean = false, error: Boolean = false) = Text(
        text,
        fontSize = 14.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(weight).padding(end = 8.dp)
    )

    companion object {
        const val OVERLAY = "F·Settings.canDrawOverlays"
        const val CHIP = "F·64dp 칩 (오른쪽 위)"
        private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.KOREA)
    }
}
