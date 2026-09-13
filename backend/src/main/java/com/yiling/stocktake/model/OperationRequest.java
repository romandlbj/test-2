package com.yiling.stocktake.model;

import java.util.ArrayList;
import java.util.List;

public class OperationRequest {
    private List<Long> ids = new ArrayList<>();
    private String reason;
    private StocktakeQuery query;
    private String format;
    private String mode;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public StocktakeQuery getQuery() { return query; }
    public void setQuery(StocktakeQuery query) { this.query = query; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
