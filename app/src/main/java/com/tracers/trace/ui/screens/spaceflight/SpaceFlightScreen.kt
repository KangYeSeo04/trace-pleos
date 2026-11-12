package com.tracers.trace.ui.screens.spaceflight

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tracers.trace.R
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceLayout
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 하늘 탭 비행 화면 (다크 SpaceDark.dc.html 우주 / 라이트 SpaceLight.dc.html 구름)
 *
 * - 다크 res/raw/space_flight.mp4, 라이트 res/raw/cloud_flight.mp4를 화면 전체에 반복·음소거 재생
 *   (전체 화면 배치에서는 space_flight_wide / cloud_flight_wide)
 * - 왼쪽 위 뒤로 버튼 + 화면 탭으로 홈 복귀
 * - 모션 줄이기 설정 시 첫 프레임 정지 화면
 * - 주행 제한 중에는 영상 재생 안 됨 (즉시 정지)
 */
@Composable
fun SpaceFlightScreen(
    onNavigateBack: () -> Unit,
    isDrivingRestricted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 3분할은 600×800용, 전체 화면은 1200×800용(*_wide, FullSpaceDark·FullSpaceLightCloud) 영상
    val wide = TraceTheme.layout == TraceLayout.Wide
    val videoRes = when {
        TraceTheme.isDark -> if (wide) R.raw.space_flight_wide else R.raw.space_flight
        else -> if (wide) R.raw.cloud_flight_wide else R.raw.cloud_flight
    }

    // TODO: 모션 줄이기 설정 확인 (AccessibilityManager.isEnabled + Settings.Global.TRANSITION_ANIMATION_SCALE)
    val isReduceMotion = false

    // ExoPlayer는 화면에 머무는 동안 하나만 만든다.
    // 테마나 칸 배치가 바뀌어 영상 파일이 달라지면 같은 플레이어에서 파일만 바꾸고 재생 위치를 이어 간다
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }
    LaunchedEffect(videoRes) {
        val position = exoPlayer.currentPosition
        val videoUri = Uri.parse("android.resource://${context.packageName}/$videoRes")
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri), position)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = !isReduceMotion && !isDrivingRestricted
    }

    // 주행 제한 중에는 영상 즉시 정지
    LaunchedEffect(isDrivingRestricted) {
        if (isDrivingRestricted) {
            exoPlayer.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TraceTheme.colors.spaceBg)
            // 아무 곳이나 탭해도 홈으로 복귀
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack
            )
    ) {
        // 영상 플레이어
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        // 뒤로 버튼 (Space Dark·Light: left 20, top 20, 48 원형, 바탕 흰 12%, 테두리 1px 흰 28%, 흰 화살표)
        val colors = TraceTheme.colors
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 20.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.videoButton)
                .border(1.dp, colors.videoButtonBorder, CircleShape)
        ) {
            Icon(
                imageVector = TraceIcons.ChevronLeft,
                contentDescription = "뒤로",
                modifier = Modifier.size(22.dp),
                tint = colors.videoButtonIcon
            )
        }
    }
}

@Preview(name = "우주 · 다크", widthDp = 600, heightDp = 800)
@Composable
private fun SpaceFlightScreenDarkPreview() {
    TraceTheme(darkTheme = true) {
        SpaceFlightScreen(onNavigateBack = {})
    }
}

@Preview(name = "구름 · 라이트", widthDp = 600, heightDp = 800)
@Composable
private fun SpaceFlightScreenLightPreview() {
    TraceTheme(darkTheme = false) {
        SpaceFlightScreen(onNavigateBack = {})
    }
}

@Preview(name = "전체 화면 · 우주 · 다크", widthDp = 1200, heightDp = 800)
@Composable
private fun SpaceFlightScreenWideDarkPreview() {
    TraceTheme(darkTheme = true) {
        SpaceFlightScreen(onNavigateBack = {})
    }
}

@Preview(name = "전체 화면 · 구름 · 라이트", widthDp = 1200, heightDp = 800)
@Composable
private fun SpaceFlightScreenWideLightPreview() {
    TraceTheme(darkTheme = false) {
        SpaceFlightScreen(onNavigateBack = {})
    }
}
