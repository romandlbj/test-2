package com.yiling.stocktake.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StocktakeRow {
    private Long id;
    private LocalDate summaryDate;
    private Long hospitalId;
    private String hospitalName;
    private Long materialId;
    private String materialCode;
    private String drugName;
    private BigDecimal bagCount;
    private BigDecimal wholeParticleQty;
    private BigDecimal looseParticleQty;
    private BigDecimal actualStockParticles;
    private BigDecimal hospitalShortageParticles;
    private BigDecimal hospitalParticleQty;
    private BigDecimal factoryIncreaseBags;
    private BigDecimal hospitalLossBags;
    private BigDecimal particleUnitPrice;
    private BigDecimal hospitalLossAmount;
    private BigDecimal factoryIncreaseAmount;
    private LocalDate guidanceStartDate;
    private LocalDate guidanceEndDate;
    private BigDecimal monthlyAvgConsumption;
    private BigDecimal currentMonthDemandBags;
    private BigDecimal availableMonths;
    private BigDecimal thirtyDayPurchaseBags;
    private String status;
    private String calculationVersion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public BigDecimal getBagCount() { return bagCount; }
    public void setBagCount(BigDecimal bagCount) { this.bagCount = bagCount; }
    public BigDecimal getWholeParticleQty() { return wholeParticleQty; }
    public void setWholeParticleQty(BigDecimal wholeParticleQty) { this.wholeParticleQty = wholeParticleQty; }
    public BigDecimal getLooseParticleQty() { return looseParticleQty; }
    public void setLooseParticleQty(BigDecimal looseParticleQty) { this.looseParticleQty = looseParticleQty; }
    public BigDecimal getActualStockParticles() { return actualStockParticles; }
    public void setActualStockParticles(BigDecimal actualStockParticles) { this.actualStockParticles = actualStockParticles; }
    public BigDecimal getHospitalShortageParticles() { return hospitalShortageParticles; }
    public void setHospitalShortageParticles(BigDecimal hospitalShortageParticles) { this.hospitalShortageParticles = hospitalShortageParticles; }
    public BigDecimal getHospitalParticleQty() { return hospitalParticleQty; }
    public void setHospitalParticleQty(BigDecimal hospitalParticleQty) { this.hospitalParticleQty = hospitalParticleQty; }
    public BigDecimal getFactoryIncreaseBags() { return factoryIncreaseBags; }
    public void setFactoryIncreaseBags(BigDecimal factoryIncreaseBags) { this.factoryIncreaseBags = factoryIncreaseBags; }
    public BigDecimal getHospitalLossBags() { return hospitalLossBags; }
    public void setHospitalLossBags(BigDecimal hospitalLossBags) { this.hospitalLossBags = hospitalLossBags; }
    public BigDecimal getParticleUnitPrice() { return particleUnitPrice; }
    public void setParticleUnitPrice(BigDecimal particleUnitPrice) { this.particleUnitPrice = particleUnitPrice; }
    public BigDecimal getHospitalLossAmount() { return hospitalLossAmount; }
    public void setHospitalLossAmount(BigDecimal hospitalLossAmount) { this.hospitalLossAmount = hospitalLossAmount; }
    public BigDecimal getFactoryIncreaseAmount() { return factoryIncreaseAmount; }
    public void setFactoryIncreaseAmount(BigDecimal factoryIncreaseAmount) { this.factoryIncreaseAmount = factoryIncreaseAmount; }
    public LocalDate getGuidanceStartDate() { return guidanceStartDate; }
    public void setGuidanceStartDate(LocalDate guidanceStartDate) { this.guidanceStartDate = guidanceStartDate; }
    public LocalDate getGuidanceEndDate() { return guidanceEndDate; }
    public void setGuidanceEndDate(LocalDate guidanceEndDate) { this.guidanceEndDate = guidanceEndDate; }
    public BigDecimal getMonthlyAvgConsumption() { return monthlyAvgConsumption; }
    public void setMonthlyAvgConsumption(BigDecimal monthlyAvgConsumption) { this.monthlyAvgConsumption = monthlyAvgConsumption; }
    public BigDecimal getCurrentMonthDemandBags() { return currentMonthDemandBags; }
    public void setCurrentMonthDemandBags(BigDecimal currentMonthDemandBags) { this.currentMonthDemandBags = currentMonthDemandBags; }
    public BigDecimal getAvailableMonths() { return availableMonths; }
    public void setAvailableMonths(BigDecimal availableMonths) { this.availableMonths = availableMonths; }
    public BigDecimal getThirtyDayPurchaseBags() { return thirtyDayPurchaseBags; }
    public void setThirtyDayPurchaseBags(BigDecimal thirtyDayPurchaseBags) { this.thirtyDayPurchaseBags = thirtyDayPurchaseBags; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCalculationVersion() { return calculationVersion; }
    public void setCalculationVersion(String calculationVersion) { this.calculationVersion = calculationVersion; }
}
