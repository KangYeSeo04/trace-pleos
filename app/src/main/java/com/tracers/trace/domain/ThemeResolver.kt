package com.tracers.trace.domain

/**
 * 해·달 스위치 선택 (A-03).
 * @param dark 고른 테마가 다크인지
 * @param systemDarkWhenChosen 고를 때 차의 시스템 테마가 다크였는지
 */
data class ThemeChoice(val dark: Boolean, val systemDarkWhenChosen: Boolean)

/** 고른 뒤 시스템 테마가 바뀌었으면 그 선택은 더 이상 따르지 않는다(저장값을 지울 대상) */
fun isChoiceStale(choice: ThemeChoice, systemDark: Boolean): Boolean =
    choice.systemDarkWhenChosen != systemDark

/**
 * 지금 테마. 선택이 있고 그 뒤로 시스템 테마가 그대로면 선택을, 아니면 시스템 테마를 따른다.
 */
fun resolveDarkTheme(choice: ThemeChoice?, systemDark: Boolean): Boolean =
    if (choice != null && !isChoiceStale(choice, systemDark)) choice.dark else systemDark
