package com.yiling.stocktake.service;

import com.yiling.stocktake.repository.AuditRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 导出任务协调器。小文件直接完成并缓存下载内容，大文件在后台生成，
 * 任务状态持久化到 export_task，下载内容默认保留 30 分钟。
 */
@Service
public class ExportTaskService {
    private static final Duration RETENTION = Duration.ofMinutes(30);

    private final AuditRepository auditRepository;
    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();

    public ExportTaskService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public String registerCompleted(String module, String format, int rowCount,
                                    String fileName, String querySummary, byte[] content) {
        String taskId = auditRepository.createExportTask(module, format, rowCount, fileName, querySummary, "SUCCESS");
        files.put(taskId, new StoredFile(content, Instant.now().plus(RETENTION)));
        return taskId;
    }

    public String submit(String module, String format, String fileName, String querySummary,
                         Supplier<ExportResult> generator) {
        String taskId = auditRepository.createExportTask(module, format, 0, fileName, querySummary, "PENDING");
        CompletableFuture.runAsync(() -> {
            try {
                ExportResult result = generator.get();
                if (result == null || result.content == null || result.content.length == 0) {
                    throw new IllegalStateException("导出文件为空");
                }
                if (result.rowCount > 50000) {
                    throw new IllegalArgumentException("导出数据超过 50000 条，请缩小查询范围后重试");
                }
                files.put(taskId, new StoredFile(result.content, Instant.now().plus(RETENTION)));
                auditRepository.updateExportTask(taskId, "SUCCESS", null);
                // row count is updated separately so the initial PENDING response is immediately available.
                auditRepository.updateExportTaskRowCount(taskId, result.rowCount);
            } catch (RuntimeException ex) {
                auditRepository.updateExportTask(taskId, "FAILED", ex.getMessage());
            }
        });
        return taskId;
    }

    public byte[] content(String taskId) {
        StoredFile file = taskId == null ? null : files.get(taskId.trim());
        if (file == null) return null;
        if (Instant.now().isAfter(file.expiresAt)) {
            files.remove(taskId.trim());
            return null;
        }
        return file.content;
    }

    public boolean isExpiredOrMissing(String taskId) {
        return content(taskId) == null;
    }

    public MediaType mediaType(String format) {
        return "CSV".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv;charset=UTF-8")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    public static class ExportResult {
        private final byte[] content;
        private final int rowCount;

        public ExportResult(byte[] content, int rowCount) {
            this.content = content;
            this.rowCount = rowCount;
        }
    }

    private static class StoredFile {
        private final byte[] content;
        private final Instant expiresAt;

        private StoredFile(byte[] content, Instant expiresAt) {
            this.content = content;
            this.expiresAt = expiresAt;
        }
    }
}
