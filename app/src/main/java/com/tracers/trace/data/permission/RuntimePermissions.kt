package com.tracers.trace.data.permission

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo

/**
 * manifest에 선언한 권한 중 런타임 요청이 필요한(dangerous) 것을 기기에서 조회한다.
 * 목록을 하드코딩하지 않아 권한이 늘어나도 자동으로 따라간다. (2026.11.09 기준 CAR_SPEED, FUSED_LOCATION)
 *
 * [OPTIONAL]은 함께 요청하지만, 거절해도 수집은 시작한다 (FusedLocation은 NaviHelper 위치의 보조라서).
 */
object RuntimePermissions {

    /** 없어도 앱이 도는 권한 */
    private val OPTIONAL = setOf("pleos.car.permission.FUSED_LOCATION")

    fun required(context: Context): List<String> {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val requested = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions ?: return emptyList()
        return requested.filter { name ->
            runCatching { pm.getPermissionInfo(name, 0).protection == PermissionInfo.PROTECTION_DANGEROUS }
                .getOrDefault(false) // 기기에 정의되지 않은 권한은 요청할 수 없으므로 뺀다
        }
    }

    /** 요청할 권한 (보조 권한 포함) */
    fun toRequest(context: Context): List<String> =
        required(context).filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

    /** 수집을 시작하는 데 꼭 필요한데 아직 없는 권한 */
    fun missing(context: Context): List<String> = toRequest(context).filter { it !in OPTIONAL }
}
