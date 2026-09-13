package com.yiling.stocktake.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;

public class StocktakeQuery {
    private Long hospitalId;
    private String materialCode;
    private String drugName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private int page = 1;
    private int pageSize = 20;
    private String sortField = "summaryDate";
    private String sortOrder = "desc";
    private Set<Long> hospitalScope;

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public Set<Long> getHospitalScope() { return hospitalScope; }
    public void setHospitalScope(Collection<Long> scope) { this.hospitalScope = scope == null ? null : new LinkedHashSet<>(scope); }
}
