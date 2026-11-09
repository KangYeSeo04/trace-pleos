package com.tracers.trace.ui.screens.maintenance

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 정비 상세 화면 상태. 달력 이동과 기록 추가 양식 입력을 들고 있다.
 * 점검 주기 계산(D-01)·방문 감지(D-03)·기록 저장(D-04)은 2단계에서 data/·domain/과 연결한다.
 *
 * 화면 back stack entry에 묶여서, 뒤로 나가거나 주행 시작 복귀(A-02)로 나가면 양식 내용도 사라진다.
 * TODO(D-05): 작성 중 기록 임시 저장 (A-02 복귀 뒤 다시 들어오면 이어서 쓰기)
 */
class MaintenanceViewModel(
    initial: MaintenanceUiState = MaintenanceUiState(),
    today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        initial.copy(
            calendar = initial.calendar ?: MaintenanceCalendarLogic.initial(initial.records, today),
            form = if (initial.form.dateText.isEmpty()) {
                // 날짜 기본 오늘 (S05-2). TODO(D-05): 주행거리 기본값은 Odometer
                initial.form.copy(dateText = today.format(DATE_FORMAT))
            } else initial.form,
        )
    )
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    // ===== 방문 감지 =====

    /** TODO(D-03): 기록하기 → 양식에 방문일·장소 채우기 */
    fun onRecordVisit() {
        val visit = _uiState.value.visit ?: return
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

    // ===== 기록 추가 양식 =====

    fun onDateChange(text: String) = updateForm { it.copy(dateText = text) }
    fun onOdometerChange(text: String) = updateForm { it.copy(odometerDigits = digitsOnly(text, MAX_DIGITS)) }
    fun onPlaceChange(text: String) = updateForm { it.copy(place = text) }
    fun onCostChange(text: String) = updateForm { it.copy(costDigits = digitsOnly(text, MAX_DIGITS)) }
    fun onMemoChange(text: String) = updateForm { it.copy(memo = text.take(MaintenanceRecordForm.MEMO_MAX)) }

    fun onToggleItem(name: String) = updateForm { form ->
        val sel = form.selectedItems
        form.copy(selectedItems = if (name in sel) sel - name else sel + name)
    }

    /** TODO(D-05): Gleo AI STT로 "타이어 위치 교환했고 8만원" → 항목·비용 채우기 */
    fun onVoiceInput() = Unit

    /** TODO(D-04·D-05): 기록 저장 후 해당 항목 주기 재계산 */
    fun onSave() {
        if (!_uiState.value.form.canSave) return
    }

    private fun updateForm(block: (MaintenanceRecordForm) -> MaintenanceRecordForm) =
        _uiState.update { it.copy(form = block(it.form)) }

    companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy. M. d")
        /** 주행거리·비용 최대 자릿수 (999,999,999) */
        const val MAX_DIGITS = 9

        /** 숫자만 남기고 앞 0은 지운다 */
        internal fun digitsOnly(text: String, max: Int): String =
            text.filter { it.isDigit() }.trimStart('0').take(max)
    }
}
