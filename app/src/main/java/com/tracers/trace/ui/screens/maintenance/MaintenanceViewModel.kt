package com.tracers.trace.ui.screens.maintenance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracers.trace.data.record.MaintenanceSnapshot
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceInputMethod
import com.tracers.trace.domain.MaintenanceItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.floor

/**
 * 정비 상세 화면 상태 (S04·S05).
 *
 * - [snapshots]: 정비 기록 + 주기 계산 결과(D-01). 기록이 저장되면 새 값이 와서 항목·목록이 같이 바뀐다
 * - [saveRecord]: 기록 추가 양식 저장(D-05). 저장 뒤 양식을 비운다
 *
 * 화면 back stack entry에 묶여서, 뒤로 나가거나 주행 시작 복귀(A-02)로 나가면 양식 내용도 사라진다.
 * TODO(D-05): 작성 중 기록 임시 저장 (A-02 복귀 뒤 다시 들어오면 이어서 쓰기)
 */
class MaintenanceViewModel(
    initial: MaintenanceUiState = MaintenanceUiState(),
    private val today: LocalDate = LocalDate.now(),
    snapshots: Flow<MaintenanceSnapshot> = emptyFlow(),
    private val saveRecord: suspend (MaintenanceEntry) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        initial.copy(
            calendar = initial.calendar ?: MaintenanceCalendarLogic.initial(initial.records, today),
            form = if (initial.form.dateText.isEmpty()) blankForm(initial.form) else initial.form,
        )
    )
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    /** 지금 총 주행거리 (km, 정수). 양식 주행거리 기본값 (S05-2) */
    private var currentOdometerKm: Int? = null
    /** 사용자가 주행거리 칸을 고쳤으면 계기판 값으로 덮어쓰지 않는다 */
    private var odometerEdited = false
    /** 방문 감지 카드에서 시작한 기록이면 "자동 감지" */
    private var fromVisit = false
    private var saving = false

    init {
        viewModelScope.launch {
            snapshots.collect { snap ->
                currentOdometerKm = snap.odometerKm?.let { floor(it).toInt() }
                _uiState.update { s ->
                    s.copy(
                        items = snap.statuses.map(MaintenanceUiMapper::item),
                        records = snap.records.map(MaintenanceUiMapper::record),
                        form = if (odometerEdited) s.form else s.form.copy(odometerDigits = odometerDigits()),
                    )
                }
            }
        }
    }

    // ===== 방문 감지 =====

    /** TODO(D-03): 기록하기 → 양식에 방문일도 채우기 */
    fun onRecordVisit() {
        val visit = _uiState.value.visit ?: return
        fromVisit = true
        updateForm { it.copy(place = visit.placeName) }
    }

    /** TODO(D-03): 숨김 상태 저장 */
    fun onDismissVisit() = _uiState.update { it.copy(visit = null) }

    // ===== 달력 =====

    fun onPrevMonth() = moveMonth(-1)
    fun onNextMonth() = moveMonth(1)

    fun onSelectDate(date: LocalDate) = _uiState.update { state ->
        state.copy(calendar = state.calendar?.copy(selectedDate = date))
    }

    private fun moveMonth(k: Long) = _uiState.update { state ->
        state.copy(calendar = state.calendar?.let { MaintenanceCalendarLogic.move(it, k) })
    }

    // ===== 기록 필터 (S04-5) =====

    /** null = 전체. 같은 항목을 다시 누르면 전체로 */
    fun onSelectFilter(name: String?) = _uiState.update { s ->
        s.copy(recordFilter = if (name == null || name == s.recordFilter) null else name)
    }

    // ===== 기록 추가 양식 =====

    fun onDateChange(text: String) = updateForm { it.copy(dateText = text) }
    fun onOdometerChange(text: String) {
        odometerEdited = true
        updateForm { it.copy(odometerDigits = digitsOnly(text, MAX_DIGITS)) }
    }
    fun onPlaceChange(text: String) = updateForm { it.copy(place = text) }
    fun onCostChange(text: String) = updateForm { it.copy(costDigits = digitsOnly(text, MAX_DIGITS)) }
    fun onMemoChange(text: String) = updateForm { it.copy(memo = text.take(MaintenanceRecordForm.MEMO_MAX)) }

    fun onToggleItem(name: String) = updateForm { form ->
        val sel = form.selectedItems
        form.copy(selectedItems = if (name in sel) sel - name else sel + name)
    }

    /** TODO(D-05): Gleo AI STT로 "타이어 위치 교환했고 8만원" → 항목·비용 채우기 */
    fun onVoiceInput() = Unit

    /** 저장 → DB → 주기 재계산은 [snapshots]로 돌아온다. 저장되면 양식을 비운다 */
    fun onSave() {
        val form = _uiState.value.form
        val entry = toEntry(form, if (fromVisit) MaintenanceInputMethod.AUTO_DETECTED else MaintenanceInputMethod.MANUAL)
            ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            runCatching { saveRecord(entry) }
                .onSuccess {
                    odometerEdited = false
                    fromVisit = false
                    _uiState.update { it.copy(form = blankForm(it.form)) }
                }
                .onFailure { Log.w(TAG, "정비 기록 저장 실패", it) }
            saving = false
        }
    }

    private fun blankForm(base: MaintenanceRecordForm) = MaintenanceRecordForm(
        dateText = today.format(DATE_FORMAT),
        odometerDigits = odometerDigits(),
        itemOptions = base.itemOptions,
    )

    /** 지금 총 주행거리로 미리 채운다 (0이어도 채움). 모르면 빈칸 */
    private fun odometerDigits(): String = currentOdometerKm?.takeIf { it >= 0 }?.toString() ?: ""

    private fun updateForm(block: (MaintenanceRecordForm) -> MaintenanceRecordForm) =
        _uiState.update { it.copy(form = block(it.form)) }

    companion object {
        private const val TAG = "MaintenanceViewModel"
        val DATE_FORMAT = MaintenanceRecordForm.DATE_FORMAT
        /** 주행거리·비용 최대 자릿수 (999,999,999) */
        const val MAX_DIGITS = 9

        /** 숫자만 남기고 앞 0은 지운다. 0만 있으면 "0" (0 km 입력) */
        internal fun digitsOnly(text: String, max: Int): String {
            val digits = text.filter { it.isDigit() }
            if (digits.isEmpty()) return ""
            return digits.trimStart('0').ifEmpty { "0" }.take(max)
        }

        /** 양식 → 저장할 기록. 저장할 수 없으면 null */
        internal fun toEntry(form: MaintenanceRecordForm, method: MaintenanceInputMethod): MaintenanceEntry? {
            if (!form.canSave) return null
            return MaintenanceEntry(
                date = form.date ?: return null,
                odometerKm = form.odometerDigits.toIntOrNull() ?: return null,
                place = form.place.trim(),
                items = form.selectedItems.mapNotNull(MaintenanceItemType::ofChip).toSet(),
                other = MaintenanceUiMapper.OTHER_CHIP in form.selectedItems,
                costWon = form.costDigits.toIntOrNull(),
                memo = form.memo.trim(),
                method = method,
            )
        }
    }
}
