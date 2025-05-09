package com.bgpay.bgai.datasource;

public enum DataSourceType {
    MASTER("master"),
    SLAVE("slave");

    private final String value;

    DataSourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 