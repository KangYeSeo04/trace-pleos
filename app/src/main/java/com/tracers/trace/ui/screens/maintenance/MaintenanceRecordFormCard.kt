package com.tracers.trace.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceTheme

/** 기록 추가 양식 입력 콜백. 값 검사(숫자만·200자)는 ViewModel이 한다 */
data class MaintenanceFormActions(
    val onDateChange: (String) -> Unit = {},
    val onOdometerChange: (String) -> Unit = {},
    val onPlaceChange: (String) -> Unit = {},
    val onToggleItem: (String) -> Unit = {},
    val onCostChange: (String) -> Unit = {},
    val onMemoChange: (String) -> Unit = {},
    /** TODO(D-05): Gleo AI STT */
    val onVoiceInput: () -> Unit = {},
    val onSave: () -> Unit = {},
)

private val KmTransformation = ThousandsTransformation(" km")
private val WonTransformation = ThousandsTransformation("원")

/**
 * 기록 추가 양식 (S05). 디자인: 타일 바탕, 모서리 20, 안쪽 18, 세로 간격 14.
 * 메모 칸은 디자인에 없어 비용 아래에 같은 입력 칸 모양으로 넣었다(기획안 S05-6).
 * 주행 중에는 A-02 자동 복귀로 이 화면을 벗어나므로 입력할 수 없다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceRecordFormCard(
    form: MaintenanceRecordForm,
    actions: MaintenanceFormActions,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 날짜 · 주행거리 (두 칸, 간격 10)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormField(
                label = "날짜",
                value = form.dateText,
                onValueChange = actions.onDateChange,
                modifier = Modifier.weight(1f)
            )
            FormField(
                label = "주행거리",
                value = form.odometerDigits,
                onValueChange = actions.onOdometerChange,
                placeholder = "0 km",
                keyboardType = KeyboardType.Number,
                visualTransformation = KmTransformation,
                modifier = Modifier.weight(1f)
            )
        }
        FormField(label = "장소", value = form.place, onValueChange = actions.onPlaceChange)

        // 항목 칩 (여러 개 선택, 선택 = 파랑 채움)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("항목")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                form.itemOptions.forEach { name ->
                    ItemChip(name, selected = name in form.selectedItems, onClick = { actions.onToggleItem(name) })
                }
            }
        }

        FormField(
            label = "비용 (선택)",
            value = form.costDigits,
            onValueChange = actions.onCostChange,
            placeholder = "0원",
            keyboardType = KeyboardType.Number,
            visualTransformation = WonTransformation
        )
        FormField(
            label = "메모 (선택)",
            value = form.memo,
            onValueChange = actions.onMemoChange,
            counter = "${form.memo.length}/${MaintenanceRecordForm.MEMO_MAX}",
            singleLine = false
        )

        // 말로 입력 + 저장 (위 4 더 띄움)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.panel)
                    .clickable(role = Role.Button, onClick = actions.onVoiceInput)
                    .semantics { contentDescription = "말로 입력" },
                contentAlignment = Alignment.Center
            ) {
                Icon(TraceIcons.Mic, contentDescription = null, tint = colors.t1, modifier = Modifier.size(22.dp))
            }
            // 항목 1개 이상일 때만 활성. 비활성 모양은 디자인에 없어 track 바탕 + t3 글자
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (form.canSave) colors.accent else colors.track)
                    .clickable(enabled = form.canSave, role = Role.Button, onClick = actions.onSave),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "저장하고 다음 주기 계산",
                    style = type.body(16f, FontWeight.SemiBold),
                    color = if (form.canSave) colors.onAccent else colors.t3
                )
            }
        }
    }
}

/** 라벨 13sp 500 t2 */
@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = TraceTheme.type.body(13f, FontWeight.Medium), color = TraceTheme.colors.t2, modifier = modifier)
}

/**
 * 라벨 + 입력 칸 (높이 52, 모서리 12, 패널 바탕, 좌우 14, 16sp t1). 라벨과 칸 사이 6.
 * 여러 줄이면 최소 높이 52에서 글자만큼 늘어난다.
 */
@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    counter: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
) {
    val colors = TraceTheme.colors
    val textStyle = TraceTheme.type.body(16f).copy(color = colors.t1)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FieldLabel(label)
            counter?.let { Text(it, style = TraceTheme.type.number(12f, FontWeight.Medium), color = colors.t3) }
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
            ),
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.panel)
                        .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = textStyle, color = colors.t3)
                    }
                    inner()
                }
            }
        )
    }
}

/** 항목 칩: 높이 44, 좌우 16, 모서리 22. 선택 accent 바탕 흰 글자 600, 아니면 패널 바탕 t1 500 */
@Composable
internal fun ItemChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val colors = TraceTheme.colors
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) colors.accent else colors.panel)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = TraceTheme.type.body(14f, if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) colors.onAccent else colors.t1
        )
    }
}
