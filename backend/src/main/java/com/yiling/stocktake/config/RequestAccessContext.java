package com.yiling.stocktake.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Minimal server-side access boundary for the two MVP modules.
 * Production SSO can replace the header adapter without changing controllers.
 *
 * Headers:
 * X-User-Id (required when auth is enabled),
 * X-Permissions (comma separated permission codes),
 * X-Hospital-Ids (comma separated authorized hospital IDs).
 */
public final class RequestAccessContext {
    public static final String USER_ID = "X-User-Id";
    public static final String PERMISSIONS = "X-Permissions";
    public static final String HOSPITAL_IDS = "X-Hospital-Ids";
    public static final String AUTH_ENABLED = "stocktake.auth.enabled";

    private RequestAccessContext() { }

    public static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(AUTH_ENABLED, "false"));
    }

    public static void require(HttpServletRequest request, String permission) {
        if (!enabled()) return;
        if (request == null || blank(request.getHeader(USER_ID))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        Set<String> permissions = csv(request.getHeader(PERMISSIONS));
        if (!permissions.contains("*") && !permissions.contains(permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "没有执行该操作的权限");
        }
    }

    public static Set<Long> hospitalScope(HttpServletRequest request) {
        if (!enabled()) return Collections.emptySet();
        Set<String> values = csv(request == null ? null : request.getHeader(HOSPITAL_IDS));
        if (values.contains("*")) return Collections.emptySet();
        Set<Long> result = new HashSet<>();
        for (String value : values) {
            try { result.add(Long.valueOf(value)); } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    public static void requireScopeConfigured(HttpServletRequest request) {
        if (!enabled()) return;
        Set<String> values = csv(request == null ? null : request.getHeader(HOSPITAL_IDS));
        if (values.contains("*")) return;
        if (values.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "未配置医院数据权限");
        Set<Long> parsed = hospitalScope(request);
        if (parsed.isEmpty() || parsed.size() != values.size()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "医院数据权限配置无效");
        }
    }

    public static void requireHospitalScope(HttpServletRequest request, java.util.Collection<Long> hospitalIds) {
        if (!enabled() || hospitalIds == null || hospitalIds.isEmpty()) return;
        if (hasFullHospitalScope(request)) return;
        Set<Long> scope = hospitalScope(request);
        if (scope.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "未配置医院数据权限");
        for (Long hospitalId : hospitalIds) {
            if (hospitalId != null && !scope.contains(hospitalId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "包含无权访问的医院数据");
        }
    }

    public static void requireHospitalScope(HttpServletRequest request, Long hospitalId) {
        if (!enabled() || hospitalId == null) return;
        Set<Long> scope = hospitalScope(request);
        if (!scope.isEmpty() && !scope.contains(hospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该医院数据");
        }
        if (scope.isEmpty() && !csv(request == null ? null : request.getHeader(HOSPITAL_IDS)).contains("*")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "未配置医院数据权限");
        }
    }

    /** Returns true only for an explicit wildcard hospital scope. */
    public static boolean hasFullHospitalScope(HttpServletRequest request) {
        return enabled() && csv(request == null ? null : request.getHeader(HOSPITAL_IDS)).contains("*");
    }

    /**
     * Checks that the current user can access every hospital included in an
     * export task. Tasks created before scope recording was introduced are
     * intentionally rejected while authentication is enabled.
     */
    public static void requireTaskHospitalScope(HttpServletRequest request, String querySummary) {
        if (!enabled()) return;
        requireScopeConfigured(request);
        String taskScope = null;
        if (querySummary != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("^scope=([^,]*)")
                    .matcher(querySummary);
            if (matcher.find()) taskScope = matcher.group(1).trim();
        }
        if (taskScope == null || taskScope.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "导出任务缺少医院数据范围");
        }
        if (hasFullHospitalScope(request)) return;
        if ("*".equals(taskScope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权下载该导出任务");
        }
        Set<Long> current = hospitalScope(request);
        for (String value : taskScope.split("\\|")) {
            try {
                if (!current.contains(Long.valueOf(value.trim()))) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权下载该导出任务");
                }
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "导出任务医院数据范围无效");
            }
        }
    }

    private static Set<String> csv(String value) {
        if (blank(value)) return Collections.emptySet();
        Set<String> result = new HashSet<>();
        for (String item : value.split(",")) if (!blank(item)) result.add(item.trim());
        return result;
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
