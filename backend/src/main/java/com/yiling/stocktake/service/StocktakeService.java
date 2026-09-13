package com.yiling.stocktake.service;

import com.yiling.stocktake.model.PageResponse;
import com.yiling.stocktake.model.StocktakeQuery;
import com.yiling.stocktake.model.StocktakeRow;
import com.yiling.stocktake.repository.AuditRepository;
import com.yiling.stocktake.repository.StocktakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StocktakeService {
    private static final int MAX_IDS = 5000;
    private static final int MAX_EXPORT_ROWS = 50000;

    private final StocktakeRepository repository;
    private final AuditRepository auditRepository;

    public StocktakeService(StocktakeRepository repository, AuditRepository auditRepository) {
        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    public PageResponse<StocktakeRow> page(StocktakeQuery query) {
        if (query == null) query = new StocktakeQuery();
        normalize(query);
        List<StocktakeRow> items = repository.find(query);
        long total = repository.count(query);
        List<StocktakeRow> all = repository.findAll(query);
        audit("QUERY", requestSummary(null, query), items.size(), "SUCCESS", null);
        return new PageResponse<>(items, total, query.getPage(), query.getPageSize(), summarize(all), queryMeta());
    }

    public List<StocktakeRow> all(StocktakeQuery query) {
        if (query == null) query = new StocktakeQuery();
        normalize(query);
        return repository.findAll(query);
    }

    public List<StocktakeRow> export(StocktakeQuery query) {
        if (query == null) query = new StocktakeQuery();
        String request = requestSummary(null, query);
        try {
            normalize(query);
            List<StocktakeRow> rows = repository.findAll(query);
            if (rows.size() > MAX_EXPORT_ROWS) {
                throw new IllegalArgumentException("导出数据超过 50000 条，请缩小查询范围后重试");
            }
            audit("EXPORT", request, rows.size(), "SUCCESS", null);
            return rows;
        } catch (RuntimeException ex) {
            audit("EXPORT", request, 0, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public long count(StocktakeQuery query) {
        if (query == null) query = new StocktakeQuery();
        normalize(query);
        return repository.count(query);
    }

    /** Used by the asynchronous export worker after the row-count threshold has been checked. */
    public List<StocktakeRow> exportForTask(StocktakeQuery query) {
        if (query == null) query = new StocktakeQuery();
        normalize(query);
        return repository.findAll(query);
    }

    public Map<String, BigDecimal> summarizeRows(List<StocktakeRow> rows) {
        return summarize(rows);
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
        meta.put("dataSource", "stocktake_detail snapshot");
        return meta;
    }

    public List<StocktakeRepository.HospitalOption> hospitals(String keyword) {
        return repository.findHospitals(keyword);
    }

    public List<StocktakeRepository.HospitalOption> hospitals(String keyword, Set<Long> hospitalScope) {
        return repository.findHospitals(keyword, hospitalScope);
    }

    public Map<String, Object> detail(long id) {
        if (id <= 0) throw new IllegalArgumentException("记录 ID 必须为正数");
        StocktakeRow row = repository.findById(id);
        if (row == null) throw new java.util.NoSuchElementException("盘点库存记录不存在或已作废");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("item", row);
        result.put("calculationVersion", row.getCalculationVersion());
        result.put("formula", new LinkedHashMap<String, String>() {{
            put("actualStockParticles", "wholeParticleQty + looseParticleQty");
            put("hospitalLossAmount", "hospitalLossBags × particleUnitPrice");
        }});
        result.put("status", row.getStatus());
        result.put("auditLogs", auditRepository.findAuditLogs("STOCKTAKE", id));
        return result;
    }

    public Long hospitalId(long id) {
        StocktakeRow row = repository.findById(id);
        return row == null ? null : row.getHospitalId();
    }

    public List<Long> hospitalIds(List<Long> ids) {
        List<Long> result = new ArrayList<>();
        for (StocktakeRow row : repository.findByIds(ids)) result.add(row.getHospitalId());
        return result;
    }

    @Transactional
    public Map<String, Object> aggregate(List<Long> ids, StocktakeQuery query) {
        String request = requestSummary(ids, query);
        try {
            boolean idMode = ids != null && !ids.isEmpty();
            if (!idMode && query == null) {
                throw new IllegalArgumentException("请选择记录或提供查询条件");
            }
            List<StocktakeRow> rows;
            if (idMode) {
                validateIds(ids);
                ensureAllIdsExist(ids);
                rows = repository.findByIds(ids);
            } else {
                if (query == null) query = new StocktakeQuery();
                normalize(query);
                rows = repository.findAll(query);
            }
            List<Map<String, Object>> errors = new ArrayList<>();
            List<Long> affectedIds = new ArrayList<>();
            for (StocktakeRow row : rows) {
                try {
                    validateRow(row);
                    affectedIds.add(row.getId());
                } catch (IllegalArgumentException ex) {
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("id", row.getId());
                    error.put("code", "STOCKTAKE_INVALID_DATA");
                    error.put("message", ex.getMessage());
                    errors.add(error);
                }
            }
            int updated = repository.markAggregated(affectedIds);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("successCount", updated);
            result.put("warningCount", 0);
            result.put("errorCount", errors.size() + Math.max(0, affectedIds.size() - updated));
            result.put("errors", errors);
            audit("AGGREGATE", request, updated, errors.isEmpty() ? "SUCCESS" : "PARTIAL", errors.isEmpty() ? null : "存在校验失败明细");
            return result;
        } catch (RuntimeException ex) {
            audit("AGGREGATE", request, 0, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public Map<String, Object> delete(List<Long> ids, String reason) {
        String request = requestSummary(ids, null);
        try {
            validateIds(ids);
            validateReason(reason);
            ensureAllIdsExist(ids);
            int affected = repository.markDeleted(ids);
            if (affected != ids.size()) {
                throw new IllegalArgumentException("部分记录已被其他操作处理，请刷新后重试");
            }
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

    private void validateRow(StocktakeRow row) {
        if (row.getBagCount() == null || row.getBagCount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("袋数不能为负数：" + row.getDrugName());
        }
        if (row.getWholeParticleQty() == null || row.getWholeParticleQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("整袋颗粒量不能为负数：" + row.getDrugName());
        }
        if (row.getLooseParticleQty() == null || row.getLooseParticleQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("零散颗粒量不能为负数：" + row.getDrugName());
        }
        if (row.getActualStockParticles() == null || row.getActualStockParticles().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("实际库存不能为负数：" + row.getDrugName());
        }
        if (row.getParticleUnitPrice() == null || row.getParticleUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("颗粒单价不能为负数：" + row.getDrugName());
        }
    }

    private void normalize(StocktakeQuery query) {
        if (query == null) throw new IllegalArgumentException("查询参数不能为空");
        if (query.getPage() < 1) query.setPage(1);
        if (query.getPageSize() < 1) query.setPageSize(20);
        if (query.getPageSize() > 200) query.setPageSize(200);
        if (query.getHospitalId() != null && query.getHospitalId() <= 0) throw new IllegalArgumentException("医院 ID 必须为正数");
        if (query.getMaterialCode() != null && query.getMaterialCode().trim().length() > 64) throw new IllegalArgumentException("物料编码长度不能超过 64 个字符");
        if (query.getDrugName() != null && query.getDrugName().trim().length() > 100) throw new IllegalArgumentException("药品名称长度不能超过 100 个字符");
        if (query.getSortField() == null || query.getSortField().trim().isEmpty()) query.setSortField("summaryDate");
        if (!java.util.Arrays.asList("summaryDate", "hospitalName", "materialCode", "drugName", "bagCount", "wholeParticleQty", "looseParticleQty", "actualStockParticles", "hospitalShortageParticles", "hospitalParticleQty", "factoryIncreaseBags", "hospitalLossBags", "particleUnitPrice", "hospitalLossAmount", "factoryIncreaseAmount", "monthlyAvgConsumption", "currentMonthDemandBags", "availableMonths", "thirtyDayPurchaseBags").contains(query.getSortField())) {
            throw new IllegalArgumentException("不支持的排序字段");
        }
        if (query.getSortOrder() == null || query.getSortOrder().trim().isEmpty()) query.setSortOrder("desc");
        if (!"asc".equalsIgnoreCase(query.getSortOrder()) && !"desc".equalsIgnoreCase(query.getSortOrder())) throw new IllegalArgumentException("排序方向只能是 asc 或 desc");
        if (query.getStartDate() != null && query.getEndDate() != null && query.getStartDate().isAfter(query.getEndDate())) {
            throw new IllegalArgumentException("汇总日期开始日期不能晚于结束日期");
        }
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().plusDays(365).isBefore(query.getEndDate())) {
            throw new IllegalArgumentException("日期范围不能超过 366 天");
        }
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
        Set<Long> existing = new HashSet<>(repository.findExistingIds(ids));
        List<Long> missing = new ArrayList<>();
        for (Long id : ids) if (!existing.contains(id)) missing.add(id);
        if (!missing.isEmpty()) throw new IllegalArgumentException("记录不存在或已作废：" + missing);
    }

    private void validateReason(String reason) {
        if (reason == null || reason.trim().length() < 2 || reason.trim().length() > 200) {
            throw new IllegalArgumentException("作废原因必填，长度为 2～200 个字符");
        }
    }

    private Map<String, BigDecimal> summarize(List<StocktakeRow> rows) {
        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        String[] fields = {"bagCount", "wholeParticleQty", "looseParticleQty", "actualStockParticles", "hospitalShortageParticles",
                "hospitalParticleQty", "factoryIncreaseBags", "hospitalLossBags", "hospitalLossAmount", "factoryIncreaseAmount",
                "monthlyAvgConsumption", "currentMonthDemandBags", "availableMonths", "thirtyDayPurchaseBags"};
        for (String field : fields) summary.put(field, BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        if (rows == null) return summary;
        BigDecimal availableStockTotal = BigDecimal.ZERO;
        BigDecimal monthlyConsumptionTotal = BigDecimal.ZERO;
        for (StocktakeRow row : rows) {
            add(summary, "bagCount", row.getBagCount());
            add(summary, "wholeParticleQty", row.getWholeParticleQty());
            add(summary, "looseParticleQty", row.getLooseParticleQty());
            add(summary, "actualStockParticles", row.getActualStockParticles());
            add(summary, "hospitalShortageParticles", row.getHospitalShortageParticles());
            add(summary, "hospitalParticleQty", row.getHospitalParticleQty());
            add(summary, "factoryIncreaseBags", row.getFactoryIncreaseBags());
            add(summary, "hospitalLossBags", row.getHospitalLossBags());
            add(summary, "hospitalLossAmount", row.getHospitalLossAmount());
            add(summary, "factoryIncreaseAmount", row.getFactoryIncreaseAmount());
            add(summary, "monthlyAvgConsumption", row.getMonthlyAvgConsumption());
            add(summary, "currentMonthDemandBags", row.getCurrentMonthDemandBags());
            if (row.getActualStockParticles() != null) availableStockTotal = availableStockTotal.add(row.getActualStockParticles().max(BigDecimal.ZERO));
            if (row.getMonthlyAvgConsumption() != null) monthlyConsumptionTotal = monthlyConsumptionTotal.add(row.getMonthlyAvgConsumption());
            add(summary, "thirtyDayPurchaseBags", row.getThirtyDayPurchaseBags());
        }
        summary.put("availableMonths", monthlyConsumptionTotal.compareTo(BigDecimal.ZERO) > 0
                ? availableStockTotal.divide(monthlyConsumptionTotal, 3, RoundingMode.HALF_UP)
                : null);
        return summary;
    }

    private void add(Map<String, BigDecimal> target, String key, BigDecimal value) {
        if (value != null) target.put(key, target.get(key).add(value));
    }

    private String requestSummary(List<Long> ids, StocktakeQuery query) {
        StringBuilder text = new StringBuilder();
        if (ids != null) text.append("ids=").append(ids);
        if (query != null) {
            if (text.length() > 0) text.append(';');
            text.append("hospitalId=").append(query.getHospitalId())
                    .append(",materialCode=").append(compact(query.getMaterialCode()))
                    .append(",drugName=").append(compact(query.getDrugName()))
                    .append(",startDate=").append(query.getStartDate())
                    .append(",endDate=").append(query.getEndDate());
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
            auditRepository.log("STOCKTAKE", operation, compact(request), affected, result, compact(failure));
        } catch (RuntimeException ignored) {
            // 审计表故障不应回滚库存业务操作。
        }
    }
}
