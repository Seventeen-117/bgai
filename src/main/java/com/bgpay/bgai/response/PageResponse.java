package com.bgpay.bgai.response;


import java.util.List;

// 泛型类，用于封装分页查询的结果
public class PageResponse<T> {
    private List<T> data;
    private long total;
    private int pageNum;
    private int pageSize;

    // 构造函数，用于初始化分页信息
    public PageResponse(List<T> data, long total, int pageNum, int pageSize) {
        this.data = data;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    // Getter 和 Setter 方法
    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}