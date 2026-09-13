package com.yiling.stocktake.controller;

import com.yiling.stocktake.model.OperationRequest;
import com.yiling.stocktake.model.PageResponse;
import com.yiling.stocktake.model.StocktakeQuery;
import com.yiling.stocktake.model.StocktakeRow;
import com.yiling.stocktake.repository.AuditRepository;
import com.yiling.stocktake.repository.StocktakeRepository;
import com.yiling.stocktake.service.StocktakeService;
import com.yiling.stocktake.service.XlsxExportService;
import com.yiling.stocktake.service.ExportTaskService;
import com.yiling.stocktake.config.RequestAccessContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/terminal/stocktake")
public class StocktakeController {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final StocktakeService service;
    private final XlsxExportService xlsxExportService;
    private final AuditRepository auditRepository;
    private final ExportTaskService exportTaskService;

    public StocktakeController(StocktakeService service, XlsxExportService xlsxExportService, AuditRepository auditRepository, ExportTaskService exportTaskService) {
        this.service = service;
        this.xlsxExportService = xlsxExportService;
        this.auditRepository = auditRepository;
        this.exportTaskService = exportTaskService;
    }

    @GetMapping
    public PageResponse<StocktakeRow> page(StocktakeQuery query, HttpServletRequest request) { RequestAccessContext.require(request, "terminal:stocktake:view"); if (query == null) query = new StocktakeQuery(); RequestAccessContext.requireScopeConfigured(request); query.setHospitalScope(RequestAccessContext.hospitalScope(request)); RequestAccessContext.requireHospitalScope(request, query.getHospitalId()); return service.page(query); }

    @GetMapping("/hospitals")
    public List<StocktakeRepository.HospitalOption> hospitals(@RequestParam(required = false) String keyword, HttpServletRequest request) { RequestAccessContext.require(request, "terminal:stocktake:view"); RequestAccessContext.requireScopeConfigured(request); return service.hospitals(keyword, RequestAccessContext.hospitalScope(request)); }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id, HttpServletRequest request) { RequestAccessContext.require(request, "terminal:stocktake:view"); RequestAccessContext.requireHospitalScope(request, service.hospitalId(id)); return service.detail(id); }

    @PostMapping("/aggregate")
    public Map<String, Object> aggregate(@RequestBody(required = false) OperationRequest request, HttpServletRequest httpRequest) {
        RequestAccessContext.require(httpRequest, "terminal:stocktake:aggregate");
        RequestAccessContext.requireScopeConfigured(httpRequest);
        RequestAccessContext.requireHospitalScope(httpRequest, request == null || request.getQuery() == null ? null : request.getQuery().getHospitalId());
        if (request != null && request.getQuery() != null) request.getQuery().setHospitalScope(RequestAccessContext.hospitalScope(httpRequest));
        if (request == null) request = new OperationRequest();
        if (request.getIds() != null) RequestAccessContext.requireHospitalScope(httpRequest, service.hospitalIds(request.getIds()));
        return service.aggregate(request.getIds(), request.getQuery());
    }

    @DeleteMapping
    public Map<String, Object> delete(@RequestBody(required = false) OperationRequest request, HttpServletRequest httpRequest) {
        RequestAccessContext.require(httpRequest, "terminal:stocktake:delete");
        RequestAccessContext.requireScopeConfigured(httpRequest);
        if (request == null) request = new OperationRequest();
        if (request.getIds() != null) RequestAccessContext.requireHospitalScope(httpRequest, service.hospitalIds(request.getIds()));
        return service.delete(request.getIds(), request.getReason());
    }

    @PostMapping("/export")
    public ResponseEntity<?> export(@RequestBody(required = false) OperationRequest request, HttpServletRequest httpRequest) {
        RequestAccessContext.require(httpRequest, "terminal:stocktake:export");
        RequestAccessContext.requireScopeConfigured(httpRequest);
        StocktakeQuery query = request == null || request.getQuery() == null ? new StocktakeQuery() : request.getQuery();
        RequestAccessContext.requireHospitalScope(httpRequest, query.getHospitalId());
        query.setHospitalScope(RequestAccessContext.hospitalScope(httpRequest));
        String format = format(request == null ? null : request.getFormat());
        String fileName = "stocktake-" + LocalDateTime.now().format(FILE_TIME) + ("XLSX".equals(format) ? ".xlsx" : ".csv");
        long expectedRows = service.count(query);
        if (expectedRows > 50000) {
            String taskId = exportTaskService.submit("STOCKTAKE", format, fileName, querySummary(query, httpRequest), () -> {
                List<StocktakeRow> rows = service.exportForTask(query);
                byte[] body = "XLSX".equals(format) ? xlsxExportService.create("盘点库存", xlsxRows(query, rows), xlsxMerges(), 6, 4, xlsxFormats()) : csvBytes(query, rows);
                return new ExportTaskService.ExportResult(body, rows.size());
            });
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("taskId", taskId);
            result.put("status", "PENDING");
            result.put("rowCount", expectedRows);
            return ResponseEntity.status(HttpStatus.ACCEPTED).header("X-Export-Task-Id", taskId).body(result);
        }
        List<StocktakeRow> rows = service.export(query);
        byte[] body = "XLSX".equals(format) ? xlsxExportService.create("盘点库存", xlsxRows(query, rows), xlsxMerges(), 6, 4, xlsxFormats()) : csvBytes(query, rows);
        MediaType contentType = "XLSX".equals(format) ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") : MediaType.parseMediaType("text/csv;charset=UTF-8");
        String taskId = exportTaskService.registerCompleted("STOCKTAKE", format, rows.size(), fileName, querySummary(query, httpRequest), body);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName).header("X-Export-Task-Id", taskId).contentType(contentType).body(body);
    }

    private byte[] csvBytes(StocktakeQuery query, List<StocktakeRow> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("配方颗粒库存盘点导出\n");
        csv.append("导出时间,").append(csv(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))).append('\n');
        csv.append("查询条件,").append(csv(querySummary(query))).append('\n');
        csv.append(String.join(",", headers())).append('\n');
        for (StocktakeRow row : rows) csv.append(String.join(",", values(row))).append('\n');
        Map<String, BigDecimal> summary = service.summarizeRows(rows);
        csv.append("统计汇总,,,,").append(value(summary.get("bagCount"))).append(',').append(value(summary.get("wholeParticleQty"))).append(',').append(value(summary.get("looseParticleQty"))).append(',').append(value(summary.get("actualStockParticles"))).append(',').append(value(summary.get("hospitalParticleQty"))).append(',').append(value(summary.get("hospitalLossBags"))).append(',').append(value(summary.get("hospitalLossAmount"))).append(',').append(value(summary.get("factoryIncreaseBags"))).append(',').append(value(summary.get("factoryIncreaseAmount"))).append(',').append(value(summary.get("monthlyAvgConsumption"))).append(',').append(value(summary.get("currentMonthDemandBags"))).append(',').append(value(summary.get("availableMonths"))).append(',').append(value(summary.get("thirtyDayPurchaseBags"))).append('\n');
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    private List<List<Object>> xlsxRows(StocktakeQuery query, List<StocktakeRow> rows) {
        List<List<Object>> result = new ArrayList<>();
        result.add(Arrays.<Object>asList("配方颗粒库存盘点导出"));
        result.add(Arrays.<Object>asList("导出时间", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        result.add(Arrays.<Object>asList("查询条件", querySummary(query)));
        result.add(Arrays.<Object>asList("单位说明", "数量=颗粒；袋数=袋；金额=元；日期=YYYY-MM-DD"));
        result.add(Arrays.<Object>asList("基础数据", "", "", "", "整袋库存", "", "零散库存", "实际库存", "医院库存", "", "亏涨结果", "", "", "", "", "要货指导", ""));
        result.add(new ArrayList<Object>(headers()));
        for (StocktakeRow row : rows) result.add(new ArrayList<Object>(rawValues(row)));
        Map<String, BigDecimal> summary = service.summarizeRows(rows);
        result.add(Arrays.<Object>asList("统计汇总", "", "", "", summary.get("bagCount"), summary.get("wholeParticleQty"), summary.get("looseParticleQty"), summary.get("actualStockParticles"), summary.get("hospitalParticleQty"), summary.get("hospitalLossBags"), summary.get("hospitalLossAmount"), summary.get("factoryIncreaseBags"), summary.get("factoryIncreaseAmount"), summary.get("monthlyAvgConsumption"), summary.get("currentMonthDemandBags"), summary.get("availableMonths"), summary.get("thirtyDayPurchaseBags")));
        return result;
    }

    private List<String> xlsxMerges() {
        return Arrays.asList("A1:Q1", "A5:D5", "E5:F5", "G5:G6", "H5:H6", "I5:J5", "K5:O5", "P5:Q5");
    }

    private Map<Integer, String> xlsxFormats() {
        Map<Integer, String> formats = new LinkedHashMap<>();
        formats.put(11, "amount");
        formats.put(13, "amount");
        return formats;
    }

    private List<String> headers() { return Arrays.asList("汇总日期", "医院名称", "物料编码", "药品名称", "袋数", "整袋颗粒量", "零散颗粒量", "实际库存(颗粒量)", "医院库存颗粒量", "医院亏(袋)", "医院亏金额", "药厂涨(袋)", "药厂涨金额", "月均消耗", "本月需求量(袋)", "现场剩余可用时长(月)", "30天需进货量(袋)"); }
    private List<Object> rawValues(StocktakeRow row) { return Arrays.<Object>asList(row.getSummaryDate(), row.getHospitalName(), row.getMaterialCode(), row.getDrugName(), row.getBagCount(), row.getWholeParticleQty(), row.getLooseParticleQty(), row.getActualStockParticles(), row.getHospitalParticleQty(), row.getHospitalLossBags(), row.getHospitalLossAmount(), row.getFactoryIncreaseBags(), row.getFactoryIncreaseAmount(), row.getMonthlyAvgConsumption(), row.getCurrentMonthDemandBags(), row.getAvailableMonths(), row.getThirtyDayPurchaseBags()); }
    private List<String> values(StocktakeRow row) { List<String> result = new ArrayList<>(); for (Object value : rawValues(row)) result.add(value(value)); return result; }
    private static String format(String value) { return "CSV".equalsIgnoreCase(value) ? "CSV" : "XLSX"; }
    private static String csv(String value) { if (value == null) return ""; return value.contains(",") || value.contains("\"") || value.contains("\r") || value.contains("\n") ? "\"" + value.replace("\"", "\"\"") + "\"" : value; }
    private static String value(Object value) { return csv(value == null ? "" : String.valueOf(value)); }
    private static String querySummary(StocktakeQuery q) { return "医院=" + q.getHospitalId() + ",物料编码=" + (q.getMaterialCode() == null ? "" : q.getMaterialCode()) + ",药品名称=" + (q.getDrugName() == null ? "" : q.getDrugName()) + ",日期=" + q.getStartDate() + "至" + q.getEndDate(); }
    private static String querySummary(StocktakeQuery q, HttpServletRequest request) { return "scope=" + scopeSummary(request) + "," + querySummary(q); }
    private static String scopeSummary(HttpServletRequest request) { if (!RequestAccessContext.enabled()) return "*"; if (RequestAccessContext.hasFullHospitalScope(request)) return "*"; java.util.List<Long> ids = new java.util.ArrayList<>(RequestAccessContext.hospitalScope(request)); java.util.Collections.sort(ids); return String.join("|", ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList())); }
}

