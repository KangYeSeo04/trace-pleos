package com.tracers.trace.domain.region

/**
 * regions_lookup.json 읽기. 새 라이브러리 없이 domain에서 쓰려고 이 파일 형식에 필요한 만큼만 JSON을 읽는다.
 * 숫자 배열은 바로 DoubleArray로 모아 상자(boxing) 없이 읽는다 (고리 좌표 약 23만 개).
 *
 * 앱에서는 시작할 때 한 번만 부른다.
 */
object RegionLookupParser {

    fun parse(json: String): RegionLookupData {
        @Suppress("UNCHECKED_CAST")
        val root = Reader(json).readRoot() as Map<String, Any?>
        val regions = (root["regions"] as List<*>).map { r ->
            r as Map<*, *>
            RegionInfo(
                code = r["code"] as String,
                name = r["name"] as String,
                sido = r["sido"] as String,
                group = r["group"] as String,
            )
        }
        val units = (root["units"] as List<*>).map { u ->
            u as Map<*, *>
            UnitShape(
                code = u["code"] as String,
                name = u["name"] as String,
                sidoShort = u["sidoShort"] as String,
                regionCode = u["region"] as String,
                bbox = u["bbox"] as DoubleArray,
                center = u["center"] as DoubleArray,
                rings = (u["rings"] as List<*>).map { it as DoubleArray },
            )
        }
        return RegionLookupData(regions, units)
    }

    /** 객체 → Map, 배열 → (숫자만이면) DoubleArray 또는 List, 문자열, 숫자(Double), true/false/null */
    private class Reader(private val s: String) {
        private var i = 0

        fun readRoot(): Any? {
            val v = value()
            ws()
            require(i == s.length) { "JSON 끝에 남은 글자 (위치 $i)" }
            return v
        }

        private fun value(): Any? {
            ws()
            return when (val c = s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) num() else error("JSON 값이 아님: '$c' (위치 $i)")
            }
        }

        private fun obj(): Map<String, Any?> {
            i++ // {
            val m = HashMap<String, Any?>()
            ws()
            if (s[i] == '}') { i++; return m }
            while (true) {
                ws()
                val k = str()
                ws(); expect(':')
                m[k] = value()
                ws()
                when (s[i++]) {
                    ',' -> continue
                    '}' -> return m
                    else -> error("객체 구분자 아님 (위치 ${i - 1})")
                }
            }
        }

        private fun arr(): Any {
            i++ // [
            ws()
            if (s[i] == ']') { i++; return emptyList<Any?>() }
            val c = s[i]
            if (c == '-' || c.isDigit()) return numArr()
            val l = ArrayList<Any?>()
            while (true) {
                l.add(value())
                ws()
                when (s[i++]) {
                    ',' -> continue
                    ']' -> return l
                    else -> error("배열 구분자 아님 (위치 ${i - 1})")
                }
            }
        }

        private fun numArr(): DoubleArray {
            var buf = DoubleArray(16)
            var n = 0
            while (true) {
                ws()
                if (n == buf.size) buf = buf.copyOf(n * 2)
                buf[n++] = num()
                ws()
                when (s[i++]) {
                    ',' -> continue
                    ']' -> return buf.copyOf(n)
                    else -> error("숫자 배열 구분자 아님 (위치 ${i - 1})")
                }
            }
        }

        private fun num(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            return s.substring(start, i).toDouble()
        }

        private fun str(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                when (val c = s[i++]) {
                    '"' -> return sb.toString()
                    '\\' -> when (val e = s[i++]) {
                        '"', '\\', '/' -> sb.append(e)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> error("알 수 없는 이스케이프 \\$e")
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun literal(word: String, v: Any?): Any? {
            require(s.startsWith(word, i)) { "JSON 값이 아님 (위치 $i)" }
            i += word.length
            return v
        }

        private fun expect(c: Char) {
            require(s[i] == c) { "'$c' 기대 (위치 $i)" }
            i++
        }

        private fun ws() {
            while (i < s.length && s[i].isWhitespace()) i++
        }
    }
}
