package com.yiling.stocktake.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 统一写入两个终端模块的操作审计日志。
 *
 * 审计写入采用独立的小事务调用方程式；调用方会吞掉审计写入异常，
 * 避免日志表异常反向阻断库存业务操作。
 */
@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String operationType, String requestSummary,
                    int affectedCount, String result, String failureReason) {
        jdbc.update("INSERT INTO operation_audit_log "
                        + "(module, operation_type, request_summary, affected_count, result, failure_reason) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                module, operationType, requestSummary, affectedCount, result, failureReason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createExportTask(String module, String format, int rowCount,
                                   String fileName, String querySummary) {
        return createExportTask(module, format, rowCount, fileName, querySummary, "SUCCESS");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createExportTask(String module, String format, int rowCount,
                                   String fileName, String querySummary, String status) {
        String id = UUID.randomUUID().toString();
        String normalizedStatus = status == null || status.trim().isEmpty() ? "PENDING" : status.trim().toUpperCase();
        boolean completed = "SUCCESS".equals(normalizedStatus) || "FAILED".equals(normalizedStatus);
        jdbc.update("INSERT INTO export_task "
                        + "(id, module, format, status, row_count, file_name, query_summary, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, module, format, normalizedStatus, rowCount, fileName, querySummary, completed ? new java.sql.Timestamp(System.currentTimeMillis()) : null);
        return id;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateExportTask(String id, String status, String failureReason) {
        if (id == null || id.trim().isEmpty()) return;
        String normalizedStatus = status == null || status.trim().isEmpty() ? "FAILED" : status.trim().toUpperCase();
        jdbc.update("UPDATE export_task SET status=?, failure_reason=?, completed_at=? WHERE id=?",
                normalizedStatus, failureReason, ("SUCCESS".equals(normalizedStatus) || "FAILED".equals(normalizedStatus))
                ? new java.sql.Timestamp(System.currentTimeMillis()) : null, id.trim());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateExportTaskRowCount(String id, int rowCount) {
        if (id == null || id.trim().isEmpty()) return;
        jdbc.update("UPDATE export_task SET row_count=? WHERE id=?", rowCount, id.trim());
    }

    public List<Map<String, Object>> findAuditLogs(String module, long recordId) {
        String needle = "%" + recordId + "%";
        return jdbc.queryForList("SELECT id, module, operation_type AS operationType, request_summary AS requestSummary, "
                        + "affected_count AS affectedCount, result, failure_reason AS failureReason, created_at AS createdAt "
                        + "FROM operation_audit_log WHERE module=? AND request_summary LIKE ? ORDER BY created_at DESC, id DESC LIMIT 50",
                module, needle);
    }

    public List<Map<String, Object>> findAuditLogs(String module, String requestNeedle) {
        String needle = "%" + (requestNeedle == null ? "" : requestNeedle) + "%";
        return jdbc.queryForList("SELECT id, module, operation_type AS operationType, request_summary AS requestSummary, "
                        + "affected_count AS affectedCount, result, failure_reason AS failureReason, created_at AS createdAt "
                        + "FROM operation_audit_log WHERE module=? AND request_summary LIKE ? ORDER BY created_at DESC, id DESC LIMIT 50",
                module, needle);
    }

    public Map<String, Object> findExportTask(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        try {
            return jdbc.queryForMap("SELECT id, module, format, status, row_count AS rowCount, file_name AS fileName, query_summary AS querySummary, created_at AS createdAt, completed_at AS completedAt, failure_reason AS failureReason FROM export_task WHERE id=?", id.trim());
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }
}
