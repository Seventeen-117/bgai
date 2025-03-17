package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.UsageCalculationDTO;

public interface MessageProducer {
    void sendBillingMessage(UsageCalculationDTO dto, String userId);
}