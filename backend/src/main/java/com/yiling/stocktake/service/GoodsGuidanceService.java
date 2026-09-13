package com.yiling.stocktake.service;

import com.yiling.stocktake.model.GoodsGuidanceRow;
import com.yiling.stocktake.model.PageResponse;
import com.yiling.stocktake.model.StocktakeQuery;
import com.yiling.stocktake.repository.AuditRepository;
import com.yiling.stocktake.repository.GoodsGuidanceRepository;
import com.yiling.stocktake.repository.StocktakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.NoSuchElementException;

@Service
public class GoodsGuidanceService {
    private static final int MAX_IDS = 5000;
    private static final int MAX_EXPORT_ROWS = 50000;

    private final GoodsGuidanceRepository repo;
    private final AuditRepository auditRepository;

    public GoodsGuidanceService(GoodsGuidanceRepository repo, AuditRepository auditRepository) {
        this.repo = repo;
        this.auditRepository = auditRepository;
    }

    public PageResponse<GoodsGuidanceRow> page(StocktakeQuery q) {
        if (q == null) q = new StocktakeQuery();
        normalize(q);
        List<GoodsGuidanceRow> rows = repo.find(q);
        List<GoodsGuidanceRow> all = repo.all(q);
        audit("QUERY", requestSummary(null, q), rows.size(), "SUCCESS", null);
        return new PageResponse<>(rows, repo.count(q), q.getPage(), q.getPageSize(), summary(all), queryMeta());
    }

    public List<GoodsGuidanceRow> all(StocktakeQuery q) {
        if (q == null) q = new StocktakeQuery();
        normalize(q);
        return repo.all(q);
    }

    public List<GoodsGuidanceRow> export(StocktakeQuery q) {
        if (q == null) q = new StocktakeQuery();
        String request = requestSummary(null, q);
        try {
            normalize(q);
            List<GoodsGuidanceRow> rows = repo.all(q);
            if (rows.size() > MAX_EXPORT_ROWS) throw new IllegalArgumentException("导出数据超过 50000 条，请缩小查询范围后重试");
            audit("EXPORT", request, rows.size(), "SUCCESS", null);
            return rows;
        } catch (RuntimeException ex) {
            audit("EXPORT", request, 0, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public long count(StocktakeQuery q) {
        if (q == null) q = new StocktakeQuery();
        normalize(q);
        return repo.count(q);
    }

    public List<GoodsGuidanceRow> exportForTask(StocktakeQuery q) {
        if (q == null) q = new StocktakeQuery();
        normalize(q);
        return repo.all(q);
    }

    public List<StocktakeRepository.HospitalOption> hospitals(String keyword) {
        return repo.findHospitals(keyword);
    }

    public List<StocktakeRepository.HospitalOption> hospitals(String keyword, Set<Long> hospitalScope) {
        return repo.findHospitals(keyword, hospitalScope);
    }

    public Map<String, Object> detail(long id) {
        if (id <= 0) throw new IllegalArgumentException("记录 ID 必须为正数");
        GoodsGuidanceRow row = repo.findById(id);
        if (row == null) throw new NoSuchElementException("要货指导记录不存在或已作废");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("item", row);
        result.put("calculationVersion", row.getCalculationVersion());
        result.put("sourceAsOf", new LinkedHashMap<String, Object>() {{
            put("openingStock", row.getOpeningStockAsOf());
            put("hospitalSupply", row.getHospitalSupplyAsOf());
            put("terminalSales", row.getTerminalSalesAsOf());
        }});
        result.put("warnings", row.getWarningCodes());
        result.put("formula", new LinkedHashMap<String, String>() {{
            put("theoreticalStockParticles", "openingStockParticles + hospitalSupplyParticles - terminalSalesParticles");
            put("monthlyAvgConsumption", "terminalSalesParticles × 30 / validConsumptionDays（有效销量数据）");
            put("currentMonthDemandBags", "monthlyAvgConsumption × daysInMonth / (30 × packParticleQty)");
            put("availableMonths", "max(stockBasis=ACTUAL_HOSPITAL ? hospitalParticleQty : theoreticalStockParticles, 0) / monthlyAvgConsumption");
            put("thirtyDayPurchaseBags", "ceil(max(monthlyAvgConsumption - max(stockBasis=ACTUAL_HOSPITAL ? hospitalParticleQty : theoreticalStockParticles, 0), 0) / packParticleQty)");
        }});
        result.put("auditLogs", auditRepository.findAuditLogs("GOODS_GUIDANCE", id));
        return result;
    }

    public Long hospitalId(long id) {
        GoodsGuidanceRow row = repo.findById(id);
        return row == null ? null : row.getHospitalId();
    }

    public List<Long> hospitalIds(List<Long> ids) {
        List<Long> result = new ArrayList<>();
        for (GoodsGuidanceRow row : repo.byIds(ids)) result.add(row.getHospitalId());
        return result;
    }

    public Map<String, BigDecimal> summarizeRows(List<GoodsGuidanceRow> rows) {
        return summary(rows);
    }

    private Map<String, Object> queryMeta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("calculationVersion", "v1");
        Map<String, Object> units = new LinkedHashMap<>();
        units.put("quantityUnit", "颗粒");
        units.put("purchaseUnit", "袋");
        units.put("amountUnit", "元");
        units.put("monthBaseDays", 30);
        meta.put("unitMeta", units);
        meta.put("dataSource", "COMPAT_STOCKTAKE_SNAPSHOT");
        meta.put("sourceNote", "优先读取期初库存、医院供货和终端销量来源流水；缺少来源时兼容回退到盘点快照字段并标记告警");
        meta.put("missingConsumptionPolicy", "EXCLUDE_FROM_AVERAGE");
        return meta;
    }

    @Transactional
    public Map<String, Object> aggregate(List<Long> ids, StocktakeQuery q) {
        String request = requestSummary(ids, q);
        try {
            boolean idMode = ids != null && !ids.isEmpty();
            if (idMode) {
                validateIds(ids);
                ensureAllIdsExist(ids);
            } else if (q == null) {
                throw new IllegalArgumentException("请选择记录或提供查询条件");
            }
            List<GoodsGuidanceRow> rows;
            if (idMode) rows = repo.byIds(ids);
            else {
                if (q == null) q = new StocktakeQuery();
                normalize(q);
                rows = repo.all(q);
            }
            int warningCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();
            List<Map<String, Object>> warnings = new ArrayList<>();
            List<Long> rowIds = new ArrayList<>();
            List<GoodsGuidanceRow> validRows = new ArrayList<>();
            for (GoodsGuidanceRow row : rows) {
                String warningText = row.getWarnings() == null ? "" : row.getWarnings();
                if (!warningText.trim().isEmpty()) {
                    warningCount++;
                    for (String code : row.getWarningCodes()) {
                        Map<String, Object> warning = new LinkedHashMap<>();
                        warning.put("id", row.getId());
                        warning.put("code", code);
                        warning.put("message", warningMessage(code, row));
                        warnings.add(warning);
                    }
                }
                if (warningText.contains("GG_UNIT_CONFIG_MISSING") || warningText.contains("GG_UNIT_CONFIG_CONFLICT") || warningText.contains("GG_NEGATIVE_STOCK")) {
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("id", row.getId());
                    String code = warningText.contains("GG_UNIT_CONFIG_MISSING") ? "GG_UNIT_CONFIG_MISSING" : warningText.contains("GG_UNIT_CONFIG_CONFLICT") ? "GG_UNIT_CONFIG_CONFLICT" : "GG_NEGATIVE_STOCK";
                    error.put("code", code);
                    error.put("message", warningMessage(code, row));
                    errors.add(error);
                } else {
                    rowIds.add(row.getId());
                    validRows.add(row);
                }
            }
            int success = repo.persistCalculatedRows(validRows);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("successCount", success);
            result.put("warningCount", warningCount);
            result.put("errorCount", errors.size() + Math.max(0, rowIds.size() - success));
            result.put("errors", errors);
            result.put("warnings", warnings);
            result.put("calculationVersion", "v1");
            result.put("dataSource", "COMPAT_STOCKTAKE_SNAPSHOT");
            audit("AGGREGATE", request, success, errors.isEmpty() ? "SUCCESS" : "PARTIAL", errors.isEmpty() ? null : "存在校验失败明细");
            return result;
        } catch (RuntimeException ex) {
            audit("AGGREGATE", request, 0, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private String warningMessage(String code, GoodsGuidanceRow row) {
        if ("GG_UNIT_CONFIG_MISSING".equals(code)) return "物料 " + row.getMaterialCode() + " 缺少有效的每袋颗粒数";
        if ("GG_UNIT_CONFIG_CONFLICT".equals(code)) return "物料 " + row.getMaterialCode() + " 在汇总日期命中多个单位换算配置";
        if ("GG_NEGATIVE_STOCK".equals(code)) return "物料 " + row.getMaterialCode() + " 理论库存为负";
        if ("GG_NO_CONSUMPTION".equals(code)) return "物料 " + row.getMaterialCode() + " 指导范围内无有效消耗数据";
        if ("GG_SOURCE_FALLBACK".equals(code)) return "物料 " + row.getMaterialCode() + " 部分来源未接入，已兼容回退盘点快照字段";
        if ("GG_UNIT_PRICE_MISSING".equals(code)) return "物料 " + row.getMaterialCode() + " 缺少有效颗粒单价";
        return code;
    }

    @Transactional
    public Map<String, Object> delete(List<Long> ids, String reason) {
        String request = requestSummary(ids, null);
        try {
            validateIds(ids);
            validateReason(reason);
            ensureAllIdsExist(ids);
            int affected = repo.delete(ids);
            if (affected != ids.size()) throw new IllegalArgumentException("部分记录已被其他操作处理，请刷新后重试");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("affectedCount", affected);
            result.put("voidedCount", affected);
            result.put("blockedCount", 0);
            result.put("blockedReasons", new Object[0]);
            audit("VOID", request + ",reason=" + compact(reason), affected, "SUCCESS", null);
            return result;
        } catch (RuntimeException ex) {
            audit("VOID", request, 0, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private void normalize(StocktakeQuery q) {
        if (q == null) throw new IllegalArgumentException("查询参数不能为空");
        if (q.getPage() < 1) q.setPage(1);
        if (q.getPageSize() < 1) q.setPageSize(20);
        if (q.getPageSize() > 200) q.setPageSize(200);
        if (q.getHospitalId() != null && q.getHospitalId() <= 0) throw new IllegalArgumentException("医院 ID 必须为正数");
        if (q.getMaterialCode() != null && q.getMaterialCode().trim().length() > 64) throw new IllegalArgumentException("物料编码长度不能超过 64 个字符");
        if (q.getDrugName() != null && q.getDrugName().trim().length() > 100) throw new IllegalArgumentException("药品名称长度不能超过 100 个字符");
        if (q.getSortField() == null || q.getSortField().trim().isEmpty()) q.setSortField("summaryDate");
        if (!java.util.Arrays.asList("summaryDate", "hospitalName", "materialCode", "drugName", "openingStockParticles", "hospitalSupplyParticles", "terminalSalesParticles", "theoreticalStockParticles", "hospitalTabletQty", "hospitalParticleQty", "hospitalGainLossQty", "particleUnitPrice", "hospitalGainLossAmount", "monthlyAvgConsumption", "currentMonthDemandBags", "availableMonths", "thirtyDayPurchaseBags").contains(q.getSortField())) throw new IllegalArgumentException("不支持的排序字段");
        if (q.getSortOrder() == null || q.getSortOrder().trim().isEmpty()) q.setSortOrder("desc");
        if (!"asc".equalsIgnoreCase(q.getSortOrder()) && !"desc".equalsIgnoreCase(q.getSortOrder())) throw new IllegalArgumentException("排序方向只能是 asc 或 desc");
        if (q.getStartDate() != null && q.getEndDate() != null && q.getStartDate().isAfter(q.getEndDate())) throw new IllegalArgumentException("汇总日期开始日期不能晚于结束日期");
        if (q.getStartDate() != null && q.getEndDate() != null && q.getStartDate().plusDays(365).isBefore(q.getEndDate())) throw new IllegalArgumentException("日期范围不能超过 366 天");
    }

    private void validateIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("请选择至少一条记录");
        if (ids.size() > MAX_IDS) throw new IllegalArgumentException("单次最多操作 5000 条记录");
        Set<Long> unique = new HashSet<>();
        for (Long id : ids) {
            if (id == null || id <= 0) throw new IllegalArgumentException("记录 ID 必须为正数");
            if (!unique.add(id)) throw new IllegalArgumentException("记录 ID 不得重复：" + id);
        }
    }

    private void ensureAllIdsExist(List<Long> ids) {
        Set<Long> existing = new HashSet<>(repo.findExistingIds(ids));
        List<Long> missing = new ArrayList<>();
        for (Long id : ids) if (!existing.contains(id)) missing.add(id);
        if (!missing.isEmpty()) throw new IllegalArgumentException("记录不存在或已作废：" + missing);
    }

    private void validateReason(String reason) {
        if (reason == null || reason.trim().length() < 2 || reason.trim().length() > 200) throw new IllegalArgumentException("作废原因必填，长度为 2～200 个字符");
    }

    private Map<String, BigDecimal> summary(List<GoodsGuidanceRow> rows) {
        String[] fields = {"openingStockParticles", "hospitalSupplyParticles", "terminalSalesParticles", "theoreticalStockParticles", "hospitalTabletQty", "hospitalParticleQty", "hospitalGainLossQty", "hospitalGainLossAmount", "monthlyAvgConsumption", "currentMonthDemandBags", "availableMonths", "thirtyDayPurchaseBags"};
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String field : fields) result.put(field, BigDecimal.ZERO.setScale(3));
        if (rows == null) return result;
        BigDecimal availableStockTotal = BigDecimal.ZERO;
        int noConsumptionCount = 0;
        BigDecimal monthlyTotal = BigDecimal.ZERO;
        for (GoodsGuidanceRow row : rows) {
            add(result, "openingStockParticles", row.getOpeningStockParticles());
            add(result, "hospitalSupplyParticles", row.getHospitalSupplyParticles());
            add(result, "terminalSalesParticles", row.getTerminalSalesParticles());
            add(result, "theoreticalStockParticles", row.getTheoreticalStockParticles());
            add(result, "hospitalTabletQty", row.getHospitalTabletQty());
            add(result, "hospitalParticleQty", row.getHospitalParticleQty());
            add(result, "hospitalGainLossQty", row.getHospitalGainLossQty());
            add(result, "hospitalGainLossAmount", row.getHospitalGainLossAmount());
            // 统计行按物料维度汇总月均消耗。每行的有效天数可能不同，直接把所有行的天数相加会把
            // 分母重复 N 次，导致汇总月均消耗被错误缩小；行级月均值相加等价于同一指导周期下
            // 的“全部有效消耗 / 有效月份数”，同时也能正确排除无有效消耗的行。
            if (row.getMonthlyAvgConsumption() != null) monthlyTotal = monthlyTotal.add(row.getMonthlyAvgConsumption());
            else noConsumptionCount++;
            add(result, "currentMonthDemandBags", row.getCurrentMonthDemandBags());
            BigDecimal availableStock = "ACTUAL_HOSPITAL".equals(row.getStockBasis()) ? row.getHospitalParticleQty() : row.getTheoreticalStockParticles();
            if (availableStock != null) {
                availableStockTotal = availableStockTotal.add(availableStock.max(BigDecimal.ZERO));
            }
            add(result, "thirtyDayPurchaseBags", row.getThirtyDayPurchaseBags());
        }
        BigDecimal monthlySummary = monthlyTotal.compareTo(BigDecimal.ZERO) > 0 ? monthlyTotal : null;
        result.put("monthlyAvgConsumption", monthlySummary);
        result.put("missingConsumptionRows", BigDecimal.valueOf(noConsumptionCount));
        result.put("availableMonths", monthlySummary != null && monthlySummary.compareTo(BigDecimal.ZERO) > 0
                ? availableStockTotal.divide(monthlySummary, 3, java.math.RoundingMode.HALF_UP)
                : null);
        return result;
    }

    private void add(Map<String, BigDecimal> result, String field, BigDecimal value) {
        if (value != null) result.put(field, result.get(field).add(value));
    }

    private String requestSummary(List<Long> ids, StocktakeQuery q) {
        StringBuilder text = new StringBuilder();
        if (ids != null) text.append("ids=").append(ids);
        if (q != null) {
            if (text.length() > 0) text.append(';');
            text.append("hospitalId=").append(q.getHospitalId()).append(",materialCode=").append(compact(q.getMaterialCode())).append(",drugName=").append(compact(q.getDrugName())).append(",startDate=").append(q.getStartDate()).append(",endDate=").append(q.getEndDate());
        }
        return compact(text.toString());
    }

    private String compact(String value) {
        if (value == null) return "";
        String result = value.replace('\n', ' ').replace('\r', ' ').trim();
        return result.length() > 500 ? result.substring(0, 500) : result;
    }

    private void audit(String operation, String request, int affected, String result, String failure) {
        try {
            auditRepository.log("GOODS_GUIDANCE", operation, compact(request), affected, result, compact(failure));
        } catch (RuntimeException ignored) {
            // 审计表故障不阻断要货指导业务操作。
        }
    }
}
