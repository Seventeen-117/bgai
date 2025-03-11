package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.UsageCalculationDTO;

import java.util.List;

public interface BillingService {
    public void processBatch(List<UsageCalculationDTO> batch);
}
