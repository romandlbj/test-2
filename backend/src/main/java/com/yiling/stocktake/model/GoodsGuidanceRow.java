package com.yiling.stocktake.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GoodsGuidanceRow {
    private Long id;
    private LocalDate summaryDate;
    private Long hospitalId;
    private String hospitalName;
    private Long materialId;
    private String materialCode;
    private String drugName;
    private BigDecimal openingStockParticles;
    private BigDecimal hospitalSupplyParticles;
    private BigDecimal terminalSalesParticles;
    private BigDecimal theoreticalStockParticles;
    private BigDecimal bookStockParticles;
    private BigDecimal hospitalTabletQty;
    private BigDecimal hospitalParticleQty;
    private BigDecimal hospitalGainLossQty;
    private BigDecimal particleUnitPrice;
    private BigDecimal hospitalGainLossAmount;
    private LocalDate guidanceStartDate;
    private LocalDate guidanceEndDate;
    private BigDecimal monthlyAvgConsumption;
    private BigDecimal currentMonthDemandBags;
    private BigDecimal availableMonths;
    private BigDecimal thirtyDayPurchaseBags;
    private BigDecimal packParticleQty;
    private String stockBasis;
    private String calculationStatus;
    private String calculationVersion;
    private String status;
    private String warnings;
    private List<String> warningCodes = new ArrayList<>();
    private String openingStockSource;
    private String hospitalSupplySource;
    private String terminalSalesSource;
    private LocalDateTime openingStockAsOf;
    private LocalDateTime hospitalSupplyAsOf;
    private LocalDateTime terminalSalesAsOf;
    private Integer validConsumptionDays;
    private Integer missingConsumptionDays;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public LocalDate getSummaryDate(){return summaryDate;} public void setSummaryDate(LocalDate v){summaryDate=v;}
    public Long getHospitalId(){return hospitalId;} public void setHospitalId(Long v){hospitalId=v;}
    public String getHospitalName(){return hospitalName;} public void setHospitalName(String v){hospitalName=v;}
    public Long getMaterialId(){return materialId;} public void setMaterialId(Long v){materialId=v;}
    public String getMaterialCode(){return materialCode;} public void setMaterialCode(String v){materialCode=v;}
    public String getDrugName(){return drugName;} public void setDrugName(String v){drugName=v;}
    public BigDecimal getOpeningStockParticles(){return openingStockParticles;} public void setOpeningStockParticles(BigDecimal v){openingStockParticles=v;}
    public BigDecimal getHospitalSupplyParticles(){return hospitalSupplyParticles;} public void setHospitalSupplyParticles(BigDecimal v){hospitalSupplyParticles=v;}
    public BigDecimal getTerminalSalesParticles(){return terminalSalesParticles;} public void setTerminalSalesParticles(BigDecimal v){terminalSalesParticles=v;}
    public BigDecimal getTheoreticalStockParticles(){return theoreticalStockParticles;} public void setTheoreticalStockParticles(BigDecimal v){theoreticalStockParticles=v;}
    public BigDecimal getBookStockParticles(){return bookStockParticles;} public void setBookStockParticles(BigDecimal v){bookStockParticles=v;}
    public BigDecimal getHospitalTabletQty(){return hospitalTabletQty;} public void setHospitalTabletQty(BigDecimal v){hospitalTabletQty=v;}
    public BigDecimal getHospitalParticleQty(){return hospitalParticleQty;} public void setHospitalParticleQty(BigDecimal v){hospitalParticleQty=v;}
    public BigDecimal getHospitalGainLossQty(){return hospitalGainLossQty;} public void setHospitalGainLossQty(BigDecimal v){hospitalGainLossQty=v;}
    public BigDecimal getParticleUnitPrice(){return particleUnitPrice;} public void setParticleUnitPrice(BigDecimal v){particleUnitPrice=v;}
    public BigDecimal getHospitalGainLossAmount(){return hospitalGainLossAmount;} public void setHospitalGainLossAmount(BigDecimal v){hospitalGainLossAmount=v;}
    public LocalDate getGuidanceStartDate(){return guidanceStartDate;} public void setGuidanceStartDate(LocalDate v){guidanceStartDate=v;}
    public LocalDate getGuidanceEndDate(){return guidanceEndDate;} public void setGuidanceEndDate(LocalDate v){guidanceEndDate=v;}
    public BigDecimal getMonthlyAvgConsumption(){return monthlyAvgConsumption;} public void setMonthlyAvgConsumption(BigDecimal v){monthlyAvgConsumption=v;}
    public BigDecimal getCurrentMonthDemandBags(){return currentMonthDemandBags;} public void setCurrentMonthDemandBags(BigDecimal v){currentMonthDemandBags=v;}
    public BigDecimal getAvailableMonths(){return availableMonths;} public void setAvailableMonths(BigDecimal v){availableMonths=v;}
    public BigDecimal getThirtyDayPurchaseBags(){return thirtyDayPurchaseBags;} public void setThirtyDayPurchaseBags(BigDecimal v){thirtyDayPurchaseBags=v;}
    public BigDecimal getPackParticleQty(){return packParticleQty;} public void setPackParticleQty(BigDecimal v){packParticleQty=v;}
    public String getStockBasis(){return stockBasis;} public void setStockBasis(String v){stockBasis=v;}
    public String getCalculationStatus(){return calculationStatus;} public void setCalculationStatus(String v){calculationStatus=v;}
    public String getCalculationVersion(){return calculationVersion;} public void setCalculationVersion(String v){calculationVersion=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getWarnings(){return warnings;} public void setWarnings(String v){warnings=v;}
    public List<String> getWarningCodes(){return warningCodes;} public void setWarningCodes(List<String> v){warningCodes=v == null ? new ArrayList<>() : v;}
    public String getOpeningStockSource(){return openingStockSource;} public void setOpeningStockSource(String v){openingStockSource=v;}
    public String getHospitalSupplySource(){return hospitalSupplySource;} public void setHospitalSupplySource(String v){hospitalSupplySource=v;}
    public String getTerminalSalesSource(){return terminalSalesSource;} public void setTerminalSalesSource(String v){terminalSalesSource=v;}
    public LocalDateTime getOpeningStockAsOf(){return openingStockAsOf;} public void setOpeningStockAsOf(LocalDateTime v){openingStockAsOf=v;}
    public LocalDateTime getHospitalSupplyAsOf(){return hospitalSupplyAsOf;} public void setHospitalSupplyAsOf(LocalDateTime v){hospitalSupplyAsOf=v;}
    public LocalDateTime getTerminalSalesAsOf(){return terminalSalesAsOf;} public void setTerminalSalesAsOf(LocalDateTime v){terminalSalesAsOf=v;}
    public Integer getValidConsumptionDays(){return validConsumptionDays;} public void setValidConsumptionDays(Integer v){validConsumptionDays=v;}
    public Integer getMissingConsumptionDays(){return missingConsumptionDays;} public void setMissingConsumptionDays(Integer v){missingConsumptionDays=v;}
}
