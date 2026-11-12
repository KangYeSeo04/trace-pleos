package com.tracers.trace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 전체 화면(1200×800) 상세 화면의 2열 배치.
 * 디자인의 `column-count: 2; column-gap: 24px`를 열마다 내용을 정해 옮긴 것.
 * 두 열은 같은 폭이고 위에 맞춘다.
 */
@Composable
fun TwoColumns(
    modifier: Modifier = Modifier,
    gap: Dp = 24.dp,
    leftWeight: Float = 1f,
    rightWeight: Float = 1f,
    left: @Composable ColumnScope.() -> Unit,
    right: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(leftWeight), content = left)
        Column(modifier = Modifier.weight(rightWeight), content = right)
    }
}
