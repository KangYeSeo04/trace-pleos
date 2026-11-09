package com.tracers.trace.ui.screens.maintenance

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 숫자만 든 입력("38800")을 "38,800 km"처럼 보여 준다. 빈 입력은 그대로 비워 placeholder가 보이게 한다.
 *
 * @param suffix 뒤에 붙일 단위 (" km", "원"). 커서는 단위 앞까지만 간다
 */
class ThousandsTransformation(private val suffix: String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val grouped = groupThousands(digits)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + commasBefore(digits.length, offset)

            override fun transformedToOriginal(offset: Int): Int {
                val o = offset.coerceAtMost(grouped.length)
                // 앞에서부터 쉼표가 아닌 글자 수
                return grouped.substring(0, o).count { it != ',' }
            }
        }
        return TransformedText(AnnotatedString(grouped + suffix), mapping)
    }

    companion object {
        internal fun groupThousands(digits: String): String = buildString {
            digits.forEachIndexed { i, c ->
                if (i > 0 && (digits.length - i) % 3 == 0) append(',')
                append(c)
            }
        }

        /** 앞에서 [offset]자리 안에 들어간 쉼표 수 */
        internal fun commasBefore(length: Int, offset: Int): Int =
            (1 until offset.coerceAtMost(length)).count { (length - it) % 3 == 0 }
    }
}
