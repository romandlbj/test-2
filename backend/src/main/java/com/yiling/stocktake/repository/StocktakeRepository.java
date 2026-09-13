package com.yiling.stocktake.repository;

import com.yiling.stocktake.model.StocktakeQuery;
import com.yiling.stocktake.model.StocktakeRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class StocktakeRepository {
    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_QUERY =
            " FROM stocktake_detail d WHERE d.deleted_at IS NULL ";

    private static final RowMapper<StocktakeRow> ROW_MAPPER = new RowMapper<StocktakeRow>() {
        @Override
        public StocktakeRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            StocktakeRow row = new StocktakeRow();
            row.setId(rs.getLong("id"));
            row.setSummaryDate(toLocalDate(rs.getDate("summary_date")));
            row.setHospitalId(rs.getLong("hospital_id"));
            row.setHospitalName(rs.getString("hospital_name"));
            row.setMaterialId(rs.getLong("material_id"));
            row.setMaterialCode(rs.getString("material_code"));
            row.setDrugName(rs.getString("drug_name"));
            row.setBagCount(rs.getBigDecimal("bag_count"));
            row.setWholeParticleQty(rs.getBigDecimal("whole_particle_qty"));
            row.setLooseParticleQty(rs.getBigDecimal("loose_particle_qty"));
            row.setActualStockParticles(rs.getBigDecimal("actual_stock_particles"));
            row.setHospitalShortageParticles(rs.getBigDecimal("hospital_shortage_particles"));
            row.setHospitalParticleQty(rs.getBigDecimal("hospital_particle_qty"));
            row.setFactoryIncreaseBags(rs.getBigDecimal("factory_increase_bags"));
            row.setHospitalLossBags(rs.getBigDecimal("hospital_loss_bags"));
            row.setParticleUnitPrice(rs.getBigDecimal("particle_unit_price"));
            row.setHospitalLossAmount(rs.getBigDecimal("hospital_loss_amount"));
            row.setFactoryIncreaseAmount(rs.getBigDecimal("factory_increase_amount"));
            row.setGuidanceStartDate(toLocalDate(rs.getDate("guidance_start_date")));
            row.setGuidanceEndDate(toLocalDate(rs.getDate("guidance_end_date")));
            row.setMonthlyAvgConsumption(rs.getBigDecimal("monthly_avg_consumption"));
            row.setCurrentMonthDemandBags(rs.getBigDecimal("current_month_demand_bags"));
            row.setAvailableMonths(rs.getBigDecimal("available_months"));
            row.setThirtyDayPurchaseBags(rs.getBigDecimal("thirty_day_purchase_bags"));
            row.setStatus(rs.getString("status"));
            row.setCalculationVersion(rs.getString("calculation_version"));
            return row;
        }
    };

    public StocktakeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<StocktakeRow> find(StocktakeQuery query) {
        QueryParts parts = buildWhere(query);
        String sortColumn = sortColumn(query.getSortField());
        String direction = "asc".equalsIgnoreCase(query.getSortOrder()) ? "ASC" : "DESC";
        int page = Math.max(1, query.getPage());
        int pageSize = Math.min(200, Math.max(1, query.getPageSize()));
        String sql = "SELECT d.* " + BASE_QUERY + parts.where +
                " ORDER BY d." + sortColumn + " " + direction + ", d.hospital_name ASC, d.drug_name ASC, d.id DESC LIMIT ? OFFSET ?";
        List<Object> args = new ArrayList<>(parts.args);
        args.add(pageSize);
        args.add((page - 1) * pageSize);
        return jdbcTemplate.query(sql, args.toArray(), ROW_MAPPER);
    }

    public long count(StocktakeQuery query) {
        QueryParts parts = buildWhere(query);
        return jdbcTemplate.queryForObject("SELECT COUNT(1) " + BASE_QUERY + parts.where,
                parts.args.toArray(), Long.class);
    }

    public List<StocktakeRow> findAll(StocktakeQuery query) {
        QueryParts parts = buildWhere(query);
        String sortColumn = sortColumn(query.getSortField());
        String direction = "asc".equalsIgnoreCase(query.getSortOrder()) ? "ASC" : "DESC";
        return jdbcTemplate.query("SELECT d.* " + BASE_QUERY + parts.where +
                " ORDER BY d." + sortColumn + " " + direction + ", d.hospital_name ASC, d.drug_name ASC, d.id DESC", parts.args.toArray(), ROW_MAPPER);
    }

    public List<StocktakeRow> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return jdbcTemplate.query("SELECT d.*" + BASE_QUERY + " AND d.id IN (" + placeholders + ") ORDER BY d.id", ids.toArray(), ROW_MAPPER);
    }

    public StocktakeRow findById(long id) {
        List<StocktakeRow> rows = jdbcTemplate.query("SELECT d.*" + BASE_QUERY + " AND d.id = ?", new Object[]{id}, ROW_MAPPER);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<HospitalOption> findHospitals(String keyword) {
        return findHospitals(keyword, Collections.emptySet());
    }

    public List<HospitalOption> findHospitals(String keyword, java.util.Set<Long> hospitalScope) {
        String value = keyword == null ? "" : keyword.trim();
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, hospital_name FROM hospital WHERE status = 'ACTIVE' AND hospital_name LIKE ?");
        args.add("%" + value + "%");
        if (hospitalScope != null && !hospitalScope.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(hospitalScope.size(), "?"));
            sql.append(" AND id IN (").append(placeholders).append(")");
            args.addAll(hospitalScope);
        }
        sql.append(" ORDER BY hospital_name LIMIT 100");
        return jdbcTemplate.query(sql.toString(), args.toArray(),
                (rs, rowNum) -> new HospitalOption(rs.getLong("id"), rs.getString("hospital_name")));
    }

    public int markDeleted(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "UPDATE stocktake_detail SET deleted_at = NOW(), status = 'VOID' " +
                "WHERE deleted_at IS NULL AND id IN (" + placeholders + ")";
        return jdbcTemplate.update(sql, ids.toArray());
    }

    public int markAggregated(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "UPDATE stocktake_detail SET actual_stock_particles = COALESCE(whole_particle_qty, 0) + COALESCE(loose_particle_qty, 0), "
                + "hospital_loss_amount = ROUND(COALESCE(hospital_loss_bags, 0) * COALESCE(particle_unit_price, 0), 2), "
                + "factory_increase_amount = ROUND(COALESCE(factory_increase_bags, 0) * COALESCE(particle_unit_price, 0), 2), "
                + "status = 'CONFIRMED', calculation_version = 'v1' " +
                "WHERE deleted_at IS NULL AND id IN (" + placeholders + ")";
        return jdbcTemplate.update(sql, ids.toArray());
    }

    public List<Long> findExistingIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return jdbcTemplate.query("SELECT id" + BASE_QUERY + " AND d.id IN (" + placeholders + ")",
                ids.toArray(), (rs, n) -> rs.getLong(1));
    }

    private QueryParts buildWhere(StocktakeQuery query) {
        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (query.getHospitalId() != null) {
            where.add(" AND d.hospital_id = ?");
            args.add(query.getHospitalId());
        }
        if (query.getHospitalScope() != null && !query.getHospitalScope().isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(query.getHospitalScope().size(), "?"));
            where.add(" AND d.hospital_id IN (" + placeholders + ")");
            args.addAll(query.getHospitalScope());
        }
        if (hasText(query.getMaterialCode())) {
            where.add(" AND d.material_code LIKE ?");
            args.add("%" + query.getMaterialCode().trim() + "%");
        }
        if (hasText(query.getDrugName())) {
            where.add(" AND d.drug_name LIKE ?");
            args.add("%" + query.getDrugName().trim() + "%");
        }
        if (query.getStartDate() != null) {
            where.add(" AND d.summary_date >= ?");
            args.add(query.getStartDate());
        }
        if (query.getEndDate() != null) {
            where.add(" AND d.summary_date <= ?");
            args.add(query.getEndDate());
        }
        return new QueryParts(String.join("", where), args);
    }

    private String sortColumn(String sortField) {
        if (sortField == null) return "summary_date";
        switch (sortField) {
            case "bagCount": return "bag_count";
            case "wholeParticleQty": return "whole_particle_qty";
            case "looseParticleQty": return "loose_particle_qty";
            case "actualStockParticles": return "actual_stock_particles";
            case "hospitalShortageParticles": return "hospital_shortage_particles";
            case "hospitalParticleQty": return "hospital_particle_qty";
            case "factoryIncreaseBags": return "factory_increase_bags";
            case "hospitalLossBags": return "hospital_loss_bags";
            case "particleUnitPrice": return "particle_unit_price";
            case "hospitalLossAmount": return "hospital_loss_amount";
            case "factoryIncreaseAmount": return "factory_increase_amount";
            case "monthlyAvgConsumption": return "monthly_avg_consumption";
            case "currentMonthDemandBags": return "current_month_demand_bags";
            case "availableMonths": return "available_months";
            case "thirtyDayPurchaseBags": return "thirty_day_purchase_bags";
            case "hospitalName": return "hospital_name";
            case "materialCode": return "material_code";
            case "drugName": return "drug_name";
            case "summaryDate":
            default: return "summary_date";
        }
    }

    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

    private static LocalDate toLocalDate(java.sql.Date date) { return date == null ? null : date.toLocalDate(); }

    private static class QueryParts {
        private final String where;
        private final List<Object> args;
        private QueryParts(String where, List<Object> args) { this.where = where; this.args = args; }
    }

    public static class HospitalOption {
        private final Long id;
        private final String hospitalName;
        public HospitalOption(Long id, String hospitalName) { this.id = id; this.hospitalName = hospitalName; }
        public Long getId() { return id; }
        public String getHospitalName() { return hospitalName; }
    }
}
