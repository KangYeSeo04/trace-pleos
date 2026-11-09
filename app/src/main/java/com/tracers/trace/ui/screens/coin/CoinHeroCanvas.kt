package com.tracers.trace.ui.screens.coin

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tracers.trace.R
import com.tracers.trace.ui.components.REDUCED_MOTION_SPEED
import com.tracers.trace.ui.components.rememberReduceMotion
import com.tracers.trace.ui.theme.TraceTheme
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas

/**
 * 코인 상단 히어로의 비행기·비행운 애니메이션.
 * CoinDark.dc.html `Core__c1_1.frame()`의 수식과 상수를 그대로 옮겼다.
 *
 * 디자인 호출값: state=cruise, drive=off, w=560, h=240, cy=92, sxr=0.7, scale=0.85, seed=coin, theme=dark.
 * 디자인의 build()는 다크에서 곧바로 반환하므로 하늘·은하수·별은 캔버스가 그리지 않고
 * 뒤의 사진(coin_bg_night)이 맡는다. 이 Composable은 비행운과 기체만 그린다.
 *
 * - JS와 같이 32ms(약 30fps)보다 짧은 간격의 프레임은 건너뛴다
 * - 모션 줄이기면 시간 진행 0.2배
 * - [animate]가 false이거나 화면이 STARTED 밖이면 프레임 루프를 멈춘다
 */
@Composable
fun CoinHeroCanvas(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val lifecycleOwner = LocalLifecycleOwner.current
    val craft = ImageBitmap.imageResource(R.drawable.shuttle)
    val trailRgb = TraceTheme.colors.heroTrail

    // JS: now = ts/1000 × (reduce ? 0.2 : 1) + t0
    var now by remember { mutableDoubleStateOf(HeroMotion.T0) }

    LaunchedEffect(animate, reduceMotion, lifecycleOwner) {
        if (!animate) return@LaunchedEffect
        val speed = if (reduceMotion) REDUCED_MOTION_SPEED.toDouble() else 1.0
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = -1L
            while (true) {
                withFrameNanos { t ->
                    if (last < 0) {
                        last = t
                    } else {
                        val dtMs = (t - last) / 1_000_000.0
                        if (dtMs >= HeroMotion.MIN_FRAME_MS) {
                            now += dtMs / 1000.0 * speed
                            last = t
                        }
                    }
                }
            }
        }
    }

    val trail = remember { TrailLayer() }

    Canvas(modifier = modifier) {
        val d = density
        val w = size.width / d
        val h = size.height / d
        trail.draw(this, w, h, now, trailRgb)
        scale(d, d, pivot = Offset.Zero) {
            HeroMotion.drawNear(this, trail.near, trail.nearCount, trailRgb)
            HeroMotion.drawCraft(this, craft, w, now)
        }
    }
}

/** 디자인의 반 해상도 비행운 캔버스(`mk(w, h, dpr * 0.5)`). 그린 뒤 확대해 부드럽게 보인다. */
private class TrailLayer {
    private var bitmap: ImageBitmap? = null
    private var canvas: GraphicsCanvas? = null
    private val scope = CanvasDrawScope()

    /** 기체 가까운(a < 6) 조각: x0, y0, x1, y1, al, lw 반복 */
    val near = FloatArray(HeroMotion.MAX_NEAR * 6)
    var nearCount = 0

    fun draw(target: DrawScope, w: Float, h: Float, now: Double, rgb: Color) {
        val k = target.density * 0.5f
        val bw = max(1, (w * k).roundToInt())
        val bh = max(1, (h * k).roundToInt())
        var bmp = bitmap
        if (bmp == null || bmp.width != bw || bmp.height != bh) {
            bmp = ImageBitmap(bw, bh)
            bitmap = bmp
            canvas = GraphicsCanvas(bmp)
        }
        val size = Size(bw.toFloat(), bh.toFloat())
        scope.draw(Density(1f), LayoutDirection.Ltr, canvas!!, size) {
            drawRect(Color.Transparent, size = size, blendMode = BlendMode.Clear)
            scale(k, k, pivot = Offset.Zero) {
                nearCount = HeroMotion.drawTrail(this, w, now, rgb, near)
            }
        }
        target.drawImage(
            image = bmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bw, bh),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(target.size.width.roundToInt(), target.size.height.roundToInt()),
            filterQuality = FilterQuality.High
        )
    }
}

/** 디자인 JS의 상수·수식. 단위는 디자인 px(= dp). */
internal object HeroMotion {

    /** 디자인 ST 표 */
    data class FlightState(
        val v: Double, val life: Double, val a: Double, val glow: Double,
        val stop: Boolean, val kinks: Boolean, val patchy: Boolean,
    )

    val CRUISE = FlightState(v = 30.0, life = 42.0, a = 0.85, glow = 1.0, stop = false, kinks = false, patchy = false)

    // 디자인 호출값 (CoinDark.dc.html __c1_1)
    private val S = CRUISE
    private const val DRIVE = false
    private const val CY = 92.0
    private const val SXR = 0.7
    private const val SC = 0.85
    private const val SEED = "coin"
    private const val STATE = "cruise"

    const val MIN_FRAME_MS = 32.0
    private const val DT = 0.14
    private const val TH0 = -0.72
    private const val EX = 0.0
    private val EY = 8.4 * SC

    /** JS: _t0 = hashStr(seed + state) % 400 + 80 */
    val T0: Double = (hashStr(SEED + STATE) % 400L + 80L).toDouble()

    private val life: Double = if (DRIVE) min(S.life, 20.0) else S.life
    private val N: Int = (life / DT).roundToInt()
    const val MAX_NEAR = 128

    private val ptX = DoubleArray(N + 1)
    private val ptY = DoubleArray(N + 1)
    private val ptA = DoubleArray(N + 1)
    private val ptS = DoubleArray(N + 1)
    private val ptOx = DoubleArray(N + 1)
    private val ptOy = DoubleArray(N + 1)

    private fun theta(s: Double) = TH0 + 0.3 * sin(0.043 * s) + 0.12 * sin(0.11 * s + 1.3)

    private fun cx(w: Float, now: Double) = w * SXR + 7 * sin(now * 0.19)
    private fun cyy(now: Double) = CY + 6 * sin(now * 0.15 + 1)
    private fun heading(now: Double) = theta(now) + (if (S.kinks) 0.05 else 0.0)

    /** 비행운을 그리고, 기체 가까운 조각을 [near]에 담아 개수를 돌려준다. */
    fun drawTrail(ds: DrawScope, w: Float, now: Double, rgb: Color, near: FloatArray): Int {
        var nearCount = 0
        val cx = cx(w, now)
        val cyy = cyy(now)
        val hd = heading(now)
        for (sd in intArrayOf(-1, 1)) {
            var px = cx + EX * cos(hd) - sd * EY * sin(hd)
            var py = cyy + EX * sin(hd) + sd * EY * cos(hd)
            for (k in 0..N) {
                val a = k * DT
                val s = now - a
                val th = theta(s)
                val v = if (S.stop) S.v * sm(5.0, 14.0, a) else S.v
                if (k > 0) { px -= cos(th) * v * DT; py -= sin(th) * v * DT }
                var lat = 0.0
                if (S.kinks && a > 4 && a < 18) lat = 9 * SC * sin(a * 2.2) * sin(Math.PI * (a - 4) / 14)
                val merge = min(1.0, a / 26) * 0.75
                val ox = -sin(th)
                val oy = cos(th)
                ptX[k] = px + ox * (lat - sd * EY * merge) + 1.7 * a
                ptY[k] = py + oy * (lat - sd * EY * merge) - 1.1 * a
                ptA[k] = a; ptS[k] = s; ptOx[k] = ox; ptOy[k] = oy
            }
            for (k in 1..N) {
                val x0 = ptX[k - 1].toFloat(); val y0 = ptY[k - 1].toFloat()
                val x1 = ptX[k].toFloat(); val y1 = ptY[k].toFloat()
                val a = ptA[k]; val s = ptS[k]; val ox = ptOx[k].toFloat(); val oy = ptOy[k].toFloat()
                if (a < 0.45) continue
                val m = 0.62 + 0.38 * vn1(s * 0.85, 13 + sd) + 0.18 * (vn1(s * 2.9, 29 + sd) - 0.5)
                var al = S.a * (1 - a / life).coerceAtLeast(0.0).pow(1.45) * sm(0.45, 1.3, a) * (0.7 + 0.3 * m)
                if (S.patchy) al *= sm(0.35, 0.7, vn1(s * 0.7, 51 + sd))
                if (al < 0.004) continue
                val lw = ((1.0 + a * 0.32) * SC * m).toFloat()
                // 다크: 그림자 선 없음
                ds.drawLine(rgb.copy(alpha = (al * 0.1).toFloat()), Offset(x0, y0), Offset(x1, y1), lw * 3.4f, StrokeCap.Round)
                for (st in -1..1) {
                    val j = if (st == 0) 0f else (st * lw * 0.42 * (0.4 + vn1(s * 1.4 + st * 5, 71 + sd))).toFloat()
                    ds.drawLine(
                        rgb.copy(alpha = (al * (if (st == 0) 0.62 else 0.3)).toFloat()),
                        Offset(x0 + ox * j, y0 + oy * j),
                        Offset(x1 + ox * j, y1 + oy * j),
                        lw * (if (st == 0) 1f else 0.65f),
                        StrokeCap.Round
                    )
                }
                if (a < 6 && nearCount < MAX_NEAR) {
                    val o = nearCount * 6
                    near[o] = x0; near[o + 1] = y0; near[o + 2] = x1; near[o + 3] = y1
                    near[o + 4] = al.toFloat(); near[o + 5] = lw
                    nearCount++
                }
            }
            if (S.kinks && sd == 1) {
                doubleArrayOf(6.5, 10.0, 13.5, 16.0).forEachIndexed { i, ag ->
                    val q = (ag / DT).roundToInt()
                    if (q > N) return@forEachIndexed
                    for (n in 0 until 5) {
                        val rr = ((6 + ag * 0.8) * SC * (0.6 + 0.6 * vn1((i * 9 + n).toDouble(), 3))).toFloat()
                        val ang = n * 1.3 + i
                        val x = (ptX[q] + cos(ang) * rr * 0.5).toFloat()
                        val y = (ptY[q] + sin(ang) * rr * 0.5).toFloat()
                        val al = (0.16 * (1 - ag / life)).toFloat()
                        ds.drawCircle(
                            Brush.radialGradient(listOf(rgb.copy(alpha = al), rgb.copy(alpha = 0f)), Offset(x, y), rr),
                            radius = rr,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        return nearCount
    }

    /** 기체 가까운 조각을 원래 해상도로 한 번 더 (JS `near.forEach`) */
    fun drawNear(ds: DrawScope, near: FloatArray, count: Int, rgb: Color) {
        for (i in 0 until count) {
            val o = i * 6
            ds.drawLine(
                rgb.copy(alpha = near[o + 4] * 0.55f),
                Offset(near[o], near[o + 1]),
                Offset(near[o + 2], near[o + 3]),
                near[o + 5] * 0.55f,
                StrokeCap.Round
            )
        }
    }

    /** 기체 사진 (기수가 +x). L = 54 × scale, 폭 = L × 425/460 */
    fun drawCraft(ds: DrawScope, img: ImageBitmap, w: Float, now: Double) {
        val l = (54 * SC).toFloat()
        val wd = l * 425f / 460f
        val cx = cx(w, now).toFloat()
        val cy = cyy(now).toFloat()
        ds.withTransform({
            translate(cx, cy)
            rotateRad(heading(now).toFloat(), pivot = Offset.Zero)
        }) {
            drawImage(
                image = img,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset((-l / 2).roundToInt(), (-wd / 2).roundToInt()),
                dstSize = IntSize(l.roundToInt(), wd.roundToInt()),
                filterQuality = FilterQuality.High
            )
        }
    }

    // ===== JS 보조 함수 그대로 =====

    /** FNV-1a (JS hashStr). 부호 없는 32비트 값 */
    fun hashStr(str: String): Long {
        var h = 2166136261L.toInt()
        for (ch in str) {
            h = h xor ch.code
            h *= 16777619
        }
        return h.toLong() and 0xFFFFFFFFL
    }

    private fun h2(ix: Int, iy: Int, seed: Int): Double {
        var h = (ix * 374761393) xor (iy * 668265263) xor (seed * 982451653)
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h.toLong() and 0xFFFFFFFFL) / 4294967295.0
    }

    /** vnoise(x, y, seed, px = 0) */
    private fun vnoise(x: Double, y: Double, seed: Int): Double {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val fx = x - x0
        val fy = y - y0
        val a = h2(x0, y0, seed)
        val b = h2(x0 + 1, y0, seed)
        val c = h2(x0, y0 + 1, seed)
        val d = h2(x0 + 1, y0 + 1, seed)
        val ux = fx * fx * (3 - 2 * fx)
        val uy = fy * fy * (3 - 2 * fy)
        return a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy
    }

    fun sm(a: Double, b: Double, x: Double): Double {
        val t = ((x - a) / (b - a)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    fun vn1(t: Double, seed: Int): Double = vnoise(t, 0.37, seed)
}
