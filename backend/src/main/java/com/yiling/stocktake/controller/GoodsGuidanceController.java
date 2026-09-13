package com.yiling.stocktake.controller;

import com.yiling.stocktake.model.GoodsGuidanceRow;
import com.yiling.stocktake.model.OperationRequest;
import com.yiling.stocktake.model.PageResponse;
import com.yiling.stocktake.model.StocktakeQuery;
import com.yiling.stocktake.repository.AuditRepository;
import com.yiling.stocktake.repository.StocktakeRepository;
import com.yiling.stocktake.service.GoodsGuidanceService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/terminal/goods-guidance")
public class GoodsGuidanceController {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final GoodsGuidanceService service;
    private final XlsxExportService xlsxExportService;
    private final AuditRepository auditRepository;
    private final ExportTaskService exportTaskService;

    public GoodsGuidanceController(GoodsGuidanceService service, XlsxExportService xlsxExportService, AuditRepository auditRepository, ExportTaskService exportTaskService) {
        this.service = service;
        this.xlsxExportService = xlsxExportService;
        this.auditRepository = auditRepository;
        this.exportTaskService = exportTaskService;
    }

    @GetMapping public PageResponse<GoodsGuidanceRow> page(StocktakeQuery q, HttpServletRequest request) { RequestAccessContext.require(request, "terminal:goods-guidance:view"); if (q == null) q = new StocktakeQuery(); RequestAccessContext.requireScopeConfigured(request); q.setHospitalScope(RequestAccessContext.hospitalScope(request)); RequestAccessContext.requireHospitalScope(request, q.getHospitalId()); return service.page(q); }
    @GetMapping("/hospitals") public List<StocktakeRepository.HospitalOption> hospitals(@RequestParam(required = false) String keyword, HttpServletRequest request) { RequestAccessContext.require(request, "terminal:goods-guidance:view"); RequestAccessContext.requireScopeConfigured(request); return service.hospitals(keyword, RequestAccessContext.hospitalScope(request)); }
    @GetMapping("/{id}") public Map<String, Object> detail(@PathVariable long id, HttpServletRequest request) { RequestAccessContext.require(request, "terminal:goods-guidance:view"); RequestAccessContext.requireHospitalScope(request, service.hospitalId(id)); return service.detail(id); }
    @PostMapping("/aggregate") public Map<String, Object> aggregate(@RequestBody(required = false) OperationRequest r, HttpServletRequest request) { RequestAccessContext.require(request, "terminal:goods-guidance:aggregate"); RequestAccessContext.requireScopeConfigured(request); if (r == null) r = new OperationRequest(); RequestAccessContext.requireHospitalScope(request, r.getQuery() == null ? null : r.getQuery().getHospitalId()); if (r.getQuery() != null) r.getQuery().setHospitalScope(RequestAccessContext.hospitalScope(request)); if (r.getIds() != null) RequestAccessContext.requireHospitalScope(request, service.hospitalIds(r.getIds())); return service.aggregate(r.getIds(), r.getQuery()); }
    @DeleteMapping public Map<String, Object> delete(@RequestBody(required = false) OperationRequest r, HttpServletRequest request) { RequestAccessContext.require(request, "terminal:goods-guidance:delete"); RequestAccessContext.requireScopeConfigured(request); if (r == null) r = new OperationRequest(); if (r.getIds() != null) RequestAccessContext.requireHospitalScope(request, service.hospitalIds(r.getIds())); return service.delete(r.getIds(), r.getReason()); }

    @PostMapping("/export")
    public ResponseEntity<?> export(@RequestBody(required = false) OperationRequest request, HttpServletRequest httpRequest) {
        RequestAccessContext.require(httpRequest, "terminal:goods-guidance:export");
        RequestAccessContext.requireScopeConfigured(httpRequest);
        StocktakeQuery q = request == null || request.getQuery() == null ? new StocktakeQuery() : request.getQuery();
        RequestAccessContext.requireHospitalScope(httpRequest, q.getHospitalId());
        q.setHospitalScope(RequestAccessContext.hospitalScope(httpRequest));
        String format = "CSV".equalsIgnoreCase(request == null ? null : request.getFormat()) ? "CSV" : "XLSX";
        String fileName = "goods-guidance-" + LocalDateTime.now().format(FILE_TIME) + ("XLSX".equals(format) ? ".xlsx" : ".csv");
        long expectedRows = service.count(q);
        if (expectedRows > 50000) {
            String taskId = exportTaskService.submit("GOODS_GUIDANCE", format, fileName, querySummary(q, httpRequest), () -> {
                List<GoodsGuidanceRow> rows = service.exportForTask(q);
                byte[] body = "XLSX".equals(format) ? xlsxExportService.create("要货指导", xlsxRows(q, rows), xlsxMerges(), 6, 4, xlsxFormats()) : csvBytes(q, rows);
                return new ExportTaskService.ExportResult(body, rows.size());
            });
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskId", taskId);
            result.put("status", "PENDING");
            result.put("rowCount", expectedRows);
            return ResponseEntity.status(HttpStatus.ACCEPTED).header("X-Export-Task-Id", taskId).body(result);
        }
        List<GoodsGuidanceRow> rows = service.export(q);
        byte[] body = "XLSX".equals(format) ? xlsxExportService.create("要货指导", xlsxRows(q, rows), xlsxMerges(), 6, 4, xlsxFormats()) : csvBytes(q, rows);
        MediaType type = "XLSX".equals(format) ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") : MediaType.parseMediaType("text/csv;charset=UTF-8");
        String taskId = exportTaskService.registerCompleted("GOODS_GUIDANCE", format, rows.size(), fileName, querySummary(q, httpRequest), body);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName).header("X-Export-Task-Id", taskId).contentType(type).body(body);
    }

    private byte[] csvBytes(StocktakeQuery q, List<GoodsGuidanceRow> rows) {
        StringBuilder s = new StringBuilder("配方颗粒要货指导导出\n");
        s.append("导出时间,").append(csv(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))).append('\n');
        s.append("查询条件,").append(csv(querySummary(q))).append('\n');
        s.append("单位说明,库存/销量/供货/医院颗粒量=颗粒；袋数=袋；金额=元\n");
        s.append(String.join(",", headers())).append('\n');
        for (GoodsGuidanceRow row : rows) s.append(String.join(",", values(row))).append('\n');
        Map<String, BigDecimal> m = service.summarizeRows(rows);
        s.append("统计汇总,,,,").append(value(m.get("openingStockParticles"))).append(',').append(value(m.get("hospitalSupplyParticles"))).append(',').append(value(m.get("terminalSalesParticles"))).append(',').append(value(m.get("theoreticalStockParticles"))).append(',').append(value(m.get("hospitalTabletQty"))).append(',').append(value(m.get("hospitalParticleQty"))).append(',').append(value(m.get("hospitalGainLossQty"))).append(',').append(',').append(value(m.get("hospitalGainLossAmount"))).append(',').append(',').append(value(m.get("monthlyAvgConsumption"))).append(',').append(value(m.get("currentMonthDemandBags"))).append(',').append(',').append(value(m.get("thirtyDayPurchaseBags"))).append('\n');
        return ("\uFEFF" + s).getBytes(StandardCharsets.UTF_8);
    }

    private List<List<Object>> xlsxRows(StocktakeQuery q, List<GoodsGuidanceRow> rows) {
        List<List<Object>> result = new ArrayList<>();
        result.add(Arrays.<Object>asList("配方颗粒要货指导导出"));
        result.add(Arrays.<Object>asList("导出时间", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        result.add(Arrays.<Object>asList("查询条件", querySummary(q)));
        result.add(Arrays.<Object>asList("单位说明", "库存/销量/供货/医院颗粒量=颗粒；袋数=袋；金额=元"));
        result.add(Arrays.<Object>asList("基础数据", "", "", "", "库存链路", "", "", "", "医院库存", "", "亏涨结果", "", "", "要货指导", "", "", "", "", ""));
        result.add(new ArrayList<Object>(headers()));
        for (GoodsGuidanceRow row : rows) result.add(new ArrayList<Object>(rawValues(row)));
        Map<String, BigDecimal> m = service.summarizeRows(rows);
        result.add(Arrays.<Object>asList("统计汇总", "", "", "", m.get("openingStockParticles"), m.get("hospitalSupplyParticles"), m.get("terminalSalesParticles"), m.get("theoreticalStockParticles"), m.get("hospitalTabletQty"), m.get("hospitalParticleQty"), m.get("hospitalGainLossQty"), "", m.get("hospitalGainLossAmount"), "", m.get("monthlyAvgConsumption"), m.get("currentMonthDemandBags"), "", m.get("thirtyDayPurchaseBags")));
        return result;
    }

    private List<String> xlsxMerges() {
        return Arrays.asList("A1:S1", "A2:B2", "A3:B3", "A4:B4", "A5:A6", "B5:B6", "C5:C6", "D5:D6", "E5:H5", "I5:J5", "K5:M5", "N5:S5");
    }

    private Map<Integer, String> xlsxFormats() {
        Map<Integer, String> formats = new LinkedHashMap<>();
        formats.put(12, "price");
        formats.put(13, "amount");
        return formats;
    }

    private List<String> headers() { return Arrays.asList("汇总日期", "医院名称", "物料编码", "药品名称", "期初库存(颗粒)", "医院供货(颗粒)", "终端销量(颗粒)", "理论库存(颗粒)", "医院饮片量", "医院颗粒量", "医院亏涨(袋)", "颗粒单价", "医院亏涨金额", "指导范围", "月均消耗(颗粒)", "本月需求量(袋)", "可用时长(月)", "30天需进货量(袋)", "计算状态"); }
    private List<Object> rawValues(GoodsGuidanceRow x) { return Arrays.<Object>asList(x.getSummaryDate(), x.getHospitalName(), x.getMaterialCode(), x.getDrugName(), x.getOpeningStockParticles(), x.getHospitalSupplyParticles(), x.getTerminalSalesParticles(), x.getTheoreticalStockParticles(), x.getHospitalTabletQty(), x.getHospitalParticleQty(), x.getHospitalGainLossQty(), x.getParticleUnitPrice(), x.getHospitalGainLossAmount(), x.getGuidanceStartDate() + " 至 " + x.getGuidanceEndDate(), x.getMonthlyAvgConsumption(), x.getCurrentMonthDemandBags(), x.getAvailableMonths(), x.getThirtyDayPurchaseBags(), x.getCalculationStatus()); }
    private List<String> values(GoodsGuidanceRow x) { List<String> result = new ArrayList<>(); for (Object value : rawValues(x)) result.add(value(value)); return result; }
    private static String csv(String v) { if (v == null) return ""; return v.contains(",") || v.contains("\"") || v.contains("\r") || v.contains("\n") ? "\"" + v.replace("\"", "\"\"") + "\"" : v; }
    private static String value(Object v) { return csv(v == null ? "" : String.valueOf(v)); }
    private static String querySummary(StocktakeQuery q) { return "医院=" + q.getHospitalId() + ",物料编码=" + (q.getMaterialCode() == null ? "" : q.getMaterialCode()) + ",药品名称=" + (q.getDrugName() == null ? "" : q.getDrugName()) + ",日期=" + q.getStartDate() + "至" + q.getEndDate(); }
    private static String querySummary(StocktakeQuery q, HttpServletRequest request) { return "scope=" + scopeSummary(request) + "," + querySummary(q); }
    private static String scopeSummary(HttpServletRequest request) { if (!RequestAccessContext.enabled()) return "*"; if (RequestAccessContext.hasFullHospitalScope(request)) return "*"; java.util.List<Long> ids = new java.util.ArrayList<>(RequestAccessContext.hospitalScope(request)); java.util.Collections.sort(ids); return String.join("|", ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList())); }
}
