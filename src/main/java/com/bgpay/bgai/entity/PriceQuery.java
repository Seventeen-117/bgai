package com.bgpay.bgai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceQuery {
    private String modelType;
    private String timePeriod;
    private String cacheStatus;
    private String ioType;
}