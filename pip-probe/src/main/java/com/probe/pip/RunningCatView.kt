package com.probe.pip

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator

/** 원 두 개(몸통+머리)와 귀·꼬리로 그린 고양이가 좌우로 계속 달린다. 크기는 View 높이에 맞춘다 */
class RunningCatView(context: Context) : View(context) {

    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 170, 60) }
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(60, 40, 30) }
    private val tail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = body.color; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val ear = Path()

    private var progress = 0f   // 0→1: 왼쪽→오른쪽, 1→0: 되돌아옴
    private var dir = 1f        // 오른쪽으로 갈 땐 +1, 돌아올 땐 -1 (그림을 좌우 반전)
    private var step = 0f       // 다리·몸 흔들림용

    private val move = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val v = it.animatedValue as Float
            if (v != progress) dir = if (v > progress) 1f else -1f
            progress = v
            invalidate()
        }
    }
    private val legs = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 400
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { step = it.animatedValue as Float }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        move.start(); legs.start()
    }

    override fun onDetachedFromWindow() {
        move.cancel(); legs.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val r = minOf(height * 0.18f, width * 0.12f)          // 몸통 반지름
        val headR = r * 0.65f
        val margin = r * 2.2f
        val x = margin + (width - 2 * margin) * progress
        val bob = kotlin.math.sin(step) * r * 0.08f
        val y = height * 0.6f + bob

        canvas.save()
        canvas.translate(x, y)
        canvas.scale(dir, 1f)

        tail.strokeWidth = r * 0.25f
        canvas.drawLine(-r * 0.9f, -r * 0.2f, -r * 1.6f, -r * (0.9f + 0.2f * kotlin.math.sin(step)), tail)

        val swing = kotlin.math.sin(step) * r * 0.35f
        for ((lx, s) in listOf(-r * 0.5f to swing, r * 0.5f to -swing)) {
            canvas.drawRect(lx + s - r * 0.12f, r * 0.6f, lx + s + r * 0.12f, r * 1.2f, body)
        }

        canvas.drawCircle(0f, 0f, r, body)

        val hx = r * 1.05f
        val hy = -r * 0.75f
        ear.reset()
        ear.moveTo(hx - headR * 0.8f, hy - headR * 0.4f); ear.lineTo(hx - headR * 0.5f, hy - headR * 1.4f); ear.lineTo(hx - headR * 0.05f, hy - headR * 0.8f); ear.close()
        ear.moveTo(hx + headR * 0.05f, hy - headR * 0.8f); ear.lineTo(hx + headR * 0.5f, hy - headR * 1.4f); ear.lineTo(hx + headR * 0.8f, hy - headR * 0.4f); ear.close()
        canvas.drawPath(ear, body)
        canvas.drawCircle(hx, hy, headR, body)
        canvas.drawCircle(hx + headR * 0.35f, hy - headR * 0.15f, headR * 0.12f, dark)

        canvas.restore()
    }

}
