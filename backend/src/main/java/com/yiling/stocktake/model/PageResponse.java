package com.yiling.stocktake.model;

import java.util.List;

public class PageResponse<T> {
    private final List<T> items;
    private final long total;
    private final int page;
    private final int pageSize;
    private final Object summary;
    private final Object queryMeta;

    public PageResponse(List<T> items, long total, int page, int pageSize, Object summary) {
        this(items, total, page, pageSize, summary, null);
    }

    public PageResponse(List<T> items, long total, int page, int pageSize, Object summary, Object queryMeta) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.summary = summary;
        this.queryMeta = queryMeta;
    }
    public List<T> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public Object getSummary() { return summary; }
    public Object getQueryMeta() { return queryMeta; }
}
