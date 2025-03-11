package com.bgpay.bgai.utils;

import com.bgpay.bgai.entity.PriceConstants;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class DecimalCalculator {
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    public static BigDecimal calculate(BigDecimal tokens, BigDecimal price) {
        return tokens.divide(PriceConstants.ONE_MILLION, MC)
                .multiply(price, MC)
                .setScale(4, RoundingMode.HALF_UP);
    }
}