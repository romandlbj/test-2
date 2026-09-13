package com.yiling.stocktake.controller;

import com.yiling.stocktake.model.ApiError;
import com.yiling.stocktake.repository.AuditRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.yiling.stocktake.service.ExportTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yiling.stocktake.config.RequestAccessContext;
import javax.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/exports/tasks")
public class ExportTaskController {
    private final AuditRepository auditRepository;
    private final ExportTaskService exportTaskService;

    public ExportTaskController(AuditRepository auditRepository, ExportTaskService exportTaskService) {
        this.auditRepository = auditRepository;
        this.exportTaskService = exportTaskService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id, HttpServletRequest request) {
        Map<String, Object> task = auditRepository.findExportTask(id);
        if (task == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("EXPORT_TASK_NOT_FOUND", "导出任务不存在或已过期"));
        requirePermission(task, request);
        RequestAccessContext.requireTaskHospitalScope(request, String.valueOf(task.get("querySummary")));
        return ResponseEntity.ok(task);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable String id, HttpServletRequest request) {
        Map<String, Object> task = auditRepository.findExportTask(id);
        if (task == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("EXPORT_TASK_NOT_FOUND", "导出任务不存在或已过期"));
        requirePermission(task, request);
        RequestAccessContext.requireTaskHospitalScope(request, String.valueOf(task.get("querySummary")));
        if (!"SUCCESS".equals(String.valueOf(task.get("status")))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("EXPORT_TASK_NOT_READY", "导出任务尚未完成"));
        }
        byte[] content = exportTaskService.content(id);
        if (content == null) return ResponseEntity.status(HttpStatus.GONE).body(new ApiError("EXPORT_TASK_EXPIRED", "导出文件已过期，请重新导出"));
        String fileName = String.valueOf(task.get("fileName"));
        String format = String.valueOf(task.get("format"));
        MediaType type = exportTaskService.mediaType(format);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName).contentType(type).body(content);
    }

    private void requirePermission(Map<String, Object> task, HttpServletRequest request) {
        String module = String.valueOf(task.get("module"));
        if ("STOCKTAKE".equalsIgnoreCase(module)) {
            RequestAccessContext.require(request, "terminal:stocktake:export");
        } else if ("GOODS_GUIDANCE".equalsIgnoreCase(module)) {
            RequestAccessContext.require(request, "terminal:goods-guidance:export");
        } else {
            RequestAccessContext.require(request, "terminal:export");
        }
    }
}
