package com.yiling.stocktake.repository;

import com.yiling.stocktake.model.GoodsGuidanceRow;
import com.yiling.stocktake.model.StocktakeQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

@Repository
public class GoodsGuidanceRepository {
    private final JdbcTemplate jdbc;
    private static final String FROM = " FROM stocktake_detail d WHERE d.deleted_at IS NULL ";
    private static final RowMapper<GoodsGuidanceRow> MAPPER = new RowMapper<GoodsGuidanceRow>() {
        public GoodsGuidanceRow mapRow(ResultSet rs, int n) throws SQLException {
            GoodsGuidanceRow r=new GoodsGuidanceRow(); r.setId(rs.getLong("id")); r.setSummaryDate(date(rs.getDate("summary_date")));
            r.setHospitalId(rs.getLong("hospital_id")); r.setHospitalName(rs.getString("hospital_name")); r.setMaterialId(rs.getLong("material_id"));
            r.setMaterialCode(rs.getString("material_code")); r.setDrugName(rs.getString("drug_name"));
            r.setGuidanceStartDate(date(rs.getDate("guidance_start_date")));
            r.setGuidanceEndDate(date(rs.getDate("guidance_end_date")));
            // 未显式配置指导范围时，按汇总日期向前滚动 90 个自然日（含首尾）返回有效范围。
            LocalDate effectiveEnd = r.getGuidanceEndDate() == null ? r.getSummaryDate() : r.getGuidanceEndDate();
            LocalDate effectiveStart = r.getGuidanceStartDate() == null && effectiveEnd != null
                    ? effectiveEnd.minusDays(89) : r.getGuidanceStartDate();
            r.setGuidanceEndDate(effectiveEnd);
            r.setGuidanceStartDate(effectiveStart);
            r.setOpeningStockParticles(rs.getBigDecimal("source_opening_stock_particles"));
            r.setOpeningStockSource(rs.getString("source_opening_stock_source"));
            r.setOpeningStockAsOf(timestamp(rs.getTimestamp("source_opening_stock_as_of")));
            r.setHospitalSupplyParticles(rs.getBigDecimal("source_hospital_supply_particles"));
            r.setHospitalSupplySource(rs.getString("source_hospital_supply_source"));
            r.setHospitalSupplyAsOf(timestamp(rs.getTimestamp("source_hospital_supply_as_of")));
            r.setTerminalSalesParticles(rs.getBigDecimal("source_terminal_sales_particles"));
            r.setTerminalSalesSource(rs.getString("source_terminal_sales_source"));
            r.setTerminalSalesAsOf(timestamp(rs.getTimestamp("source_terminal_sales_as_of")));
            int validDays = rs.getInt("source_valid_consumption_days");
            r.setValidConsumptionDays(rs.wasNull() ? 0 : validDays);
            r.setMissingConsumptionDays(r.getGuidanceStartDate() == null || r.getGuidanceEndDate() == null ? 0 :
                    (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(r.getGuidanceStartDate(), r.getGuidanceEndDate()) + 1 - r.getValidConsumptionDays()));
            java.math.BigDecimal opening = r.getOpeningStockParticles() == null ? java.math.BigDecimal.ZERO : r.getOpeningStockParticles();
            java.math.BigDecimal supply = r.getHospitalSupplyParticles() == null ? java.math.BigDecimal.ZERO : r.getHospitalSupplyParticles();
            java.math.BigDecimal sales = r.getTerminalSalesParticles() == null ? java.math.BigDecimal.ZERO : r.getTerminalSalesParticles();
            r.setTheoreticalStockParticles(opening.add(supply).subtract(sales));
            java.math.BigDecimal persistedBookStock = rs.getBigDecimal("book_stock_particles");
            r.setBookStockParticles(persistedBookStock == null ? r.getTheoreticalStockParticles() : persistedBookStock);
            r.setHospitalTabletQty(rs.getBigDecimal("hospital_shortage_particles")); r.setHospitalParticleQty(rs.getBigDecimal("hospital_particle_qty"));
            r.setStockBasis(r.getHospitalParticleQty() == null ? "THEORETICAL" : "ACTUAL_HOSPITAL");
            r.setHospitalGainLossQty(rs.getBigDecimal("hospital_loss_bags"));
            java.math.BigDecimal configuredPrice = rs.getBigDecimal("pack_config_particle_unit_price");
            r.setParticleUnitPrice(configuredPrice == null ? rs.getBigDecimal("particle_unit_price") : configuredPrice);
            r.setHospitalGainLossAmount(rs.getBigDecimal("hospital_loss_amount"));
            java.math.BigDecimal sourceSales = r.getTerminalSalesParticles();
            int sourceDays = r.getValidConsumptionDays() == null ? 0 : r.getValidConsumptionDays();
            java.math.BigDecimal monthlyFromSource = sourceSales == null || sourceDays <= 0
                    ? rs.getBigDecimal("monthly_avg_consumption")
                    : sourceSales.multiply(java.math.BigDecimal.valueOf(30)).divide(java.math.BigDecimal.valueOf(sourceDays), 3, java.math.RoundingMode.HALF_UP);
            // 无有效消耗时以 null 表示，避免把“无数据”误显示为 0 月均消耗。
            if (monthlyFromSource != null && monthlyFromSource.compareTo(java.math.BigDecimal.ZERO) <= 0) monthlyFromSource = null;
            r.setMonthlyAvgConsumption(monthlyFromSource);
            r.setCurrentMonthDemandBags(rs.getBigDecimal("current_month_demand_bags")); r.setAvailableMonths(rs.getBigDecimal("available_months"));
            java.math.BigDecimal configuredPackQty = rs.getBigDecimal("pack_particle_qty");
            r.setPackParticleQty(configuredPackQty);
            r.setThirtyDayPurchaseBags(rs.getBigDecimal("thirty_day_purchase_bags")); r.setCalculationStatus("READY"); r.setCalculationVersion(rs.getString("calculation_version")); r.setStatus(rs.getString("status"));
            java.math.BigDecimal monthly = r.getMonthlyAvgConsumption();
            if (configuredPackQty != null && configuredPackQty.compareTo(java.math.BigDecimal.ZERO) > 0 && monthly != null && monthly.compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal availableStock = "ACTUAL_HOSPITAL".equals(r.getStockBasis()) ? r.getHospitalParticleQty() : r.getTheoreticalStockParticles();
                if (availableStock == null) availableStock = java.math.BigDecimal.ZERO;
                availableStock = availableStock.max(java.math.BigDecimal.ZERO);
                r.setAvailableMonths(availableStock.divide(monthly, 3, java.math.RoundingMode.HALF_UP));
                int daysInMonth = r.getSummaryDate() == null ? 30 : java.time.YearMonth.from(r.getSummaryDate()).lengthOfMonth();
                r.setCurrentMonthDemandBags(monthly.multiply(java.math.BigDecimal.valueOf(daysInMonth)).divide(java.math.BigDecimal.valueOf(30).multiply(configuredPackQty), 3, java.math.RoundingMode.HALF_UP));
                java.math.BigDecimal replenishment = monthly.subtract(availableStock).max(java.math.BigDecimal.ZERO);
                r.setThirtyDayPurchaseBags(replenishment.divide(configuredPackQty, 0, java.math.RoundingMode.CEILING));
            } else if (monthly == null || monthly.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                r.setAvailableMonths(null);
                r.setThirtyDayPurchaseBags(java.math.BigDecimal.ZERO);
            }
            StringBuilder warnings = new StringBuilder();
            if ("stocktake_detail.actual_stock_particles_compat".equals(r.getOpeningStockSource())
                    || "NOT_CONNECTED_COMPAT_ZERO".equals(r.getHospitalSupplySource())
                    || "stocktake_detail.monthly_avg_consumption_compat".equals(r.getTerminalSalesSource())) {
                warnings.append("GG_SOURCE_FALLBACK");
            }
            if (configuredPackQty == null || configuredPackQty.compareTo(java.math.BigDecimal.ZERO) <= 0) { if (warnings.length()>0) warnings.append(","); warnings.append("GG_UNIT_CONFIG_MISSING"); }
            if (r.getMonthlyAvgConsumption()==null || r.getMonthlyAvgConsumption().compareTo(java.math.BigDecimal.ZERO)<=0) {
                if (warnings.length()>0) warnings.append(","); warnings.append("GG_NO_CONSUMPTION");
            }
            if (rs.getInt("pack_config_count") > 1) {
                if (warnings.length()>0) warnings.append(","); warnings.append("GG_UNIT_CONFIG_CONFLICT");
            }
            // 价格配置允许为 0（例如暂未定价的内部物料），只有缺失或负数才属于非法值。
            if (r.getParticleUnitPrice() == null || r.getParticleUnitPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
                if (warnings.length()>0) warnings.append(","); warnings.append("GG_UNIT_PRICE_MISSING");
            }
            if ((r.getOpeningStockParticles()!=null && r.getOpeningStockParticles().compareTo(java.math.BigDecimal.ZERO)<0)
                    || (r.getTheoreticalStockParticles()!=null && r.getTheoreticalStockParticles().compareTo(java.math.BigDecimal.ZERO)<0)) {
                if (warnings.length()>0) warnings.append(","); warnings.append("GG_NEGATIVE_STOCK");
            }
            r.setWarnings(warnings.toString());
            if (r.getWarnings() != null && !r.getWarnings().isEmpty()) {
                r.setWarningCodes(java.util.Arrays.asList(r.getWarnings().split(",")));
                r.setCalculationStatus("WARNING");
            }
            return r;
        }
    };
    public GoodsGuidanceRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private static final String SELECT = "SELECT d.*, "
            + "COALESCE((SELECT oi.particle_qty FROM opening_inventory_snapshot oi "
             + "WHERE oi.hospital_id=d.hospital_id AND oi.material_id=d.material_id AND oi.material_code=d.material_code "
             + "AND oi.opening_date <= COALESCE(d.guidance_start_date, d.summary_date) AND oi.status='VALIDATED' AND oi.deleted_at IS NULL "
            + "ORDER BY oi.opening_date DESC, oi.id DESC LIMIT 1), d.actual_stock_particles) AS source_opening_stock_particles, "
            + "CASE WHEN EXISTS (SELECT 1 FROM opening_inventory_snapshot oi "
             + "WHERE oi.hospital_id=d.hospital_id AND oi.material_id=d.material_id AND oi.material_code=d.material_code "
             + "AND oi.opening_date <= COALESCE(d.guidance_start_date, d.summary_date) AND oi.status='VALIDATED' AND oi.deleted_at IS NULL) "
            + "THEN 'opening_inventory_snapshot' ELSE 'stocktake_detail.actual_stock_particles_compat' END AS source_opening_stock_source, "
            + "(SELECT oi.created_at FROM opening_inventory_snapshot oi "
             + "WHERE oi.hospital_id=d.hospital_id AND oi.material_id=d.material_id AND oi.material_code=d.material_code "
             + "AND oi.opening_date <= COALESCE(d.guidance_start_date, d.summary_date) AND oi.status='VALIDATED' AND oi.deleted_at IS NULL "
            + "ORDER BY oi.opening_date DESC, oi.id DESC LIMIT 1) AS source_opening_stock_as_of, "
            + "COALESCE((SELECT SUM(hs.particle_qty) FROM hospital_supply_record hs "
             + "WHERE hs.hospital_id=d.hospital_id AND hs.material_id=d.material_id AND hs.material_code=d.material_code "
             + "AND hs.supply_date >= COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)) "
             + "AND hs.supply_date <= COALESCE(d.guidance_end_date, d.summary_date) "
             + "AND hs.status='VALIDATED' AND hs.deleted_at IS NULL), 0) AS source_hospital_supply_particles, "
            + "CASE WHEN EXISTS (SELECT 1 FROM hospital_supply_record hs "
             + "WHERE hs.hospital_id=d.hospital_id AND hs.material_id=d.material_id AND hs.material_code=d.material_code "
             + "AND hs.supply_date >= COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)) "
             + "AND hs.supply_date <= COALESCE(d.guidance_end_date, d.summary_date) "
             + "AND hs.status='VALIDATED' AND hs.deleted_at IS NULL) "
            + "THEN 'hospital_supply_record' ELSE 'NOT_CONNECTED_COMPAT_ZERO' END AS source_hospital_supply_source, "
            + "(SELECT MAX(hs.created_at) FROM hospital_supply_record hs "
             + "WHERE hs.hospital_id=d.hospital_id AND hs.material_id=d.material_id AND hs.material_code=d.material_code "
             + "AND hs.supply_date >= COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)) "
             + "AND hs.supply_date <= COALESCE(d.guidance_end_date, d.summary_date) "
             + "AND hs.status='VALIDATED' AND hs.deleted_at IS NULL) AS source_hospital_supply_as_of, "
            + "COALESCE((SELECT SUM(ts.particle_qty * (DATEDIFF(LEAST(ts.end_date, COALESCE(d.guidance_end_date, d.summary_date)), "
             + "GREATEST(ts.start_date, COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)))) + 1) "
            + "/ NULLIF(DATEDIFF(ts.end_date, ts.start_date) + 1, 0)) FROM terminal_sales_record ts "
            + "WHERE ts.hospital_id=d.hospital_id AND ts.material_id=d.material_id AND ts.material_code=d.material_code "
            + "AND ts.start_date <= COALESCE(d.guidance_end_date, d.summary_date) "
             + "AND ts.end_date >= COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)) "
            + "AND ts.status='VALIDATED' AND ts.deleted_at IS NULL), "
            + "d.monthly_avg_consumption * (DATEDIFF(COALESCE(d.guidance_end_date, d.summary_date), "
            + "COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY))) + 1) / 30) AS source_terminal_sales_particles, "
            + "CASE WHEN EXISTS (SELECT 1 FROM terminal_sales_record ts "
            + "WHERE ts.hospital_id=d.hospital_id AND ts.material_id=d.material_id AND ts.material_code=d.material_code "
            + "AND ts.start_date <= COALESCE(d.guidance_end_date, d.summary_date) "
             + "AND ts.end_date >= COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)) "
            + "AND ts.status='VALIDATED' AND ts.deleted_at IS NULL) "
            + "THEN 'terminal_sales_record' ELSE 'stocktake_detail.monthly_avg_consumption_compat' END AS source_terminal_sales_source, "
            + "(SELECT MAX(ts.created_at) FROM terminal_sales_record ts "
            + "WHERE ts.hospital_id=d.hospital_id AND ts.material_id=d.material_id AND ts.material_code=d.material_code "
            + "AND ts.start_date <= COALESCE(d.guidance_end_date, d.summary_date) "
             + "AND ts.end_date >= COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)) "
            + "AND ts.status='VALIDATED' AND ts.deleted_at IS NULL) AS source_terminal_sales_as_of, "
            + "COALESCE((SELECT SUM(DATEDIFF(LEAST(ts.end_date, COALESCE(d.guidance_end_date, d.summary_date)), "
             + "GREATEST(ts.start_date, COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)))) + 1) "
            + "FROM terminal_sales_record ts WHERE ts.hospital_id=d.hospital_id AND ts.material_id=d.material_id AND ts.material_code=d.material_code "
              + "AND ts.start_date <= COALESCE(d.guidance_end_date, d.summary_date) AND ts.end_date >= COALESCE(d.guidance_start_date, DATE_SUB(d.summary_date, INTERVAL 89 DAY)) "
              + "AND ts.status='VALIDATED' AND ts.deleted_at IS NULL), 0) AS source_valid_consumption_days, "
            + "(SELECT muc.pack_particle_qty FROM material_unit_config muc "
            + "WHERE muc.material_id=d.material_id AND muc.material_code=d.material_code "
            + "AND muc.effective_from <= d.summary_date "
            + "AND (muc.effective_to IS NULL OR muc.effective_to >= d.summary_date) "
            + "ORDER BY muc.effective_from DESC, muc.id DESC LIMIT 1) AS pack_particle_qty, "
            + "(SELECT muc.particle_unit_price FROM material_unit_config muc "
            + "WHERE muc.material_id=d.material_id AND muc.material_code=d.material_code "
            + "AND muc.effective_from <= d.summary_date "
            + "AND (muc.effective_to IS NULL OR muc.effective_to >= d.summary_date) "
            + "ORDER BY muc.effective_from DESC, muc.id DESC LIMIT 1) AS pack_config_particle_unit_price, "
            + "(SELECT COUNT(1) FROM material_unit_config muc "
            + "WHERE muc.material_id=d.material_id AND muc.material_code=d.material_code "
            + "AND muc.effective_from <= d.summary_date "
            + "AND (muc.effective_to IS NULL OR muc.effective_to >= d.summary_date)) AS pack_config_count";
    public List<GoodsGuidanceRow> find(StocktakeQuery q){ Query p=where(q); String order=sortExpression(q.getSortField()); String dir="asc".equalsIgnoreCase(q.getSortOrder())?"ASC":"DESC"; int page=Math.max(q.getPage(),1), size=Math.min(Math.max(q.getPageSize(),1),200); List<Object>a=new ArrayList<>(p.args);a.add(size);a.add((page-1)*size);return jdbc.query(SELECT+FROM+p.sql+" ORDER BY "+order+" "+dir+",d.hospital_name ASC,d.drug_name ASC,d.id DESC LIMIT ? OFFSET ?",a.toArray(),MAPPER); }
    public List<GoodsGuidanceRow> all(StocktakeQuery q){Query p=where(q);String order=sortExpression(q.getSortField());String dir="asc".equalsIgnoreCase(q.getSortOrder())?"ASC":"DESC";return jdbc.query(SELECT+FROM+p.sql+" ORDER BY "+order+" "+dir+",d.hospital_name ASC,d.drug_name ASC,d.id DESC",p.args.toArray(),MAPPER);}
    public List<GoodsGuidanceRow> byIds(List<Long> ids){if(ids==null||ids.isEmpty())return Collections.emptyList();String ph=String.join(",",Collections.nCopies(ids.size(),"?"));return jdbc.query(SELECT+FROM+" AND d.id IN ("+ph+") ORDER BY d.id",ids.toArray(),MAPPER);}
    public GoodsGuidanceRow findById(long id){List<GoodsGuidanceRow> rows=jdbc.query(SELECT+FROM+" AND d.id=?",new Object[]{id},MAPPER);return rows.isEmpty()?null:rows.get(0);}
    public List<StocktakeRepository.HospitalOption> findHospitals(String keyword){return findHospitals(keyword, Collections.emptySet());}
    public List<StocktakeRepository.HospitalOption> findHospitals(String keyword, Set<Long> hospitalScope){String v=keyword==null?"":keyword.trim();List<Object>a=new ArrayList<>();a.add("%"+v+"%");StringBuilder sql=new StringBuilder("SELECT id,hospital_name FROM hospital WHERE status='ACTIVE' AND hospital_name LIKE ?");if(hospitalScope!=null&&!hospitalScope.isEmpty()){String ph=String.join(",",Collections.nCopies(hospitalScope.size(),"?"));sql.append(" AND id IN (").append(ph).append(")");a.addAll(hospitalScope);}sql.append(" ORDER BY hospital_name LIMIT 100");return jdbc.query(sql.toString(),a.toArray(),(rs,n)->new StocktakeRepository.HospitalOption(rs.getLong("id"),rs.getString("hospital_name")));}
    public Map<String, Object> sourceAsOf(){
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> values = jdbc.queryForMap("SELECT MAX(opening_created_at) AS openingStock, MAX(supply_created_at) AS hospitalSupply, MAX(sales_created_at) AS terminalSales FROM (SELECT MAX(created_at) AS opening_created_at, NULL AS supply_created_at, NULL AS sales_created_at FROM opening_inventory_snapshot WHERE deleted_at IS NULL UNION ALL SELECT NULL, MAX(created_at), NULL FROM hospital_supply_record WHERE deleted_at IS NULL UNION ALL SELECT NULL, NULL, MAX(created_at) FROM terminal_sales_record WHERE deleted_at IS NULL) source_times");
        result.put("openingStock", values.get("openingStock"));
        result.put("hospitalSupply", values.get("hospitalSupply"));
        result.put("terminalSales", values.get("terminalSales"));
        return result;
    }
    public long count(StocktakeQuery q){Query p=where(q);return jdbc.queryForObject("SELECT COUNT(1)"+FROM+p.sql,p.args.toArray(),Long.class);}
    public int aggregate(List<Long> ids){if(ids==null||ids.isEmpty())return 0;String ph=String.join(",",Collections.nCopies(ids.size(),"?"));return jdbc.update("UPDATE stocktake_detail SET status='CONFIRMED',calculation_version='v1' WHERE deleted_at IS NULL AND id IN ("+ph+")",ids.toArray());}
    public int persistCalculatedRows(List<GoodsGuidanceRow> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        int total = 0;
        for (GoodsGuidanceRow row : rows) {
            total += jdbc.update("UPDATE stocktake_detail SET monthly_avg_consumption=?, current_month_demand_bags=?, available_months=?, thirty_day_purchase_bags=?, status='CONFIRMED', calculation_version='v1' WHERE deleted_at IS NULL AND id=?",
                    row.getMonthlyAvgConsumption(), row.getCurrentMonthDemandBags(), row.getAvailableMonths(), row.getThirtyDayPurchaseBags(), row.getId());
        }
        return total;
    }
    public int delete(List<Long> ids){if(ids==null||ids.isEmpty())return 0;String ph=String.join(",",Collections.nCopies(ids.size(),"?"));return jdbc.update("UPDATE stocktake_detail SET status='VOID',deleted_at=NOW() WHERE deleted_at IS NULL AND id IN ("+ph+")",ids.toArray());}
    public List<Long> findExistingIds(List<Long> ids){if(ids==null||ids.isEmpty())return Collections.emptyList();String ph=String.join(",",Collections.nCopies(ids.size(),"?"));return jdbc.query("SELECT id"+FROM+" AND d.id IN ("+ph+")",ids.toArray(),(rs,n)->rs.getLong(1));}
    private String sortExpression(String field){if(field==null)return "d.summary_date";switch(field){case "hospitalName":return "d.hospital_name";case "materialCode":return "d.material_code";case "drugName":return "d.drug_name";case "openingStockParticles":return "source_opening_stock_particles";case "theoreticalStockParticles":return "d.actual_stock_particles";case "terminalSalesParticles":return "source_terminal_sales_particles";case "monthlyAvgConsumption":return "d.monthly_avg_consumption";case "hospitalTabletQty":return "d.hospital_shortage_particles";case "hospitalParticleQty":return "d.hospital_particle_qty";case "currentMonthDemandBags":return "d.current_month_demand_bags";case "availableMonths":return "d.available_months";case "thirtyDayPurchaseBags":return "d.thirty_day_purchase_bags";case "hospitalGainLossQty":return "d.hospital_loss_bags";case "hospitalGainLossAmount":return "d.hospital_loss_amount";case "particleUnitPrice":return "d.particle_unit_price";case "summaryDate":default:return "d.summary_date";}}
    private Query where(StocktakeQuery q){List<String>w=new ArrayList<>();List<Object>a=new ArrayList<>();if(q.getHospitalId()!=null){w.add(" AND d.hospital_id=?");a.add(q.getHospitalId());}if(q.getHospitalScope()!=null&&!q.getHospitalScope().isEmpty()){String ph=String.join(",",Collections.nCopies(q.getHospitalScope().size(),"?"));w.add(" AND d.hospital_id IN ("+ph+")");a.addAll(q.getHospitalScope());}if(q.getMaterialCode()!=null&&!q.getMaterialCode().trim().isEmpty()){w.add(" AND d.material_code LIKE ?");a.add("%"+q.getMaterialCode().trim()+"%");}if(q.getDrugName()!=null&&!q.getDrugName().trim().isEmpty()){w.add(" AND d.drug_name LIKE ?");a.add("%"+q.getDrugName().trim()+"%");}if(q.getStartDate()!=null){w.add(" AND d.summary_date>=?");a.add(q.getStartDate());}if(q.getEndDate()!=null){w.add(" AND d.summary_date<=?");a.add(q.getEndDate());}return new Query(String.join("",w),a);}
    private static LocalDate date(java.sql.Date d){return d==null?null:d.toLocalDate();}
    private static java.time.LocalDateTime timestamp(java.sql.Timestamp value){return value==null?null:value.toLocalDateTime();}
    private static class Query{String sql;List<Object>args;Query(String s,List<Object>a){sql=s;args=a;}}
}
