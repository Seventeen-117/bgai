package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.UsageCalculationDTO;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public interface BillingService {
    public void processBatch(List<UsageCalculationDTO> batch,String userId);

    void processSingleRecord(UsageCalculationDTO calculationDTO,String userId);

    public void processMessage(MessageExt messageExt);


}
