package com.bgpay.bgai.service;

public interface BGAIService {
    boolean executeFirstStep(String businessKey);
    
    boolean executeSecondStep(String businessKey, boolean firstStepResult);
    
    boolean executeThirdStep(String businessKey, boolean secondStepResult);

    boolean compensateFirstStep(String businessKey);

    boolean compensateSecondStep(String businessKey);

    boolean compensateThirdStep(String businessKey);
} 