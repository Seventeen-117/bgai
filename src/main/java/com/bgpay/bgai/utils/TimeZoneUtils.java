package com.bgpay.bgai.utils;

import java.time.*;

public class TimeZoneUtils {
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime DISCOUNT_START = LocalTime.of(23, 0); // 23:00
    private static final LocalTime DISCOUNT_END = LocalTime.of(9, 0);    // 09:00

    public static ZonedDateTime getCurrentBeijingTime() {
        return ZonedDateTime.now(BEIJING_ZONE);
    }

    public static String determineTimePeriod(ZonedDateTime beijingTime) {
        LocalDate date = beijingTime.toLocalDate();
        
        ZonedDateTime discountStart = ZonedDateTime.of(date, DISCOUNT_START, BEIJING_ZONE);
        ZonedDateTime discountEnd = ZonedDateTime.of(date, DISCOUNT_END, BEIJING_ZONE);
        
        if (discountEnd.isBefore(discountStart)) {
            discountEnd = discountEnd.plusDays(1);
        }
        
        return (beijingTime.isAfter(discountStart) && beijingTime.isBefore(discountEnd))
                ? "discount" : "standard";
    }

    public static ZonedDateTime toBeijingTime(LocalDateTime utcTime) {
        return utcTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(BEIJING_ZONE);
    }

    public static boolean isInDiscountPeriod(LocalDateTime utcTime) {
        ZonedDateTime beijingTime = toBeijingTime(utcTime);
        LocalTime time = beijingTime.toLocalTime();

        return (time.isAfter(LocalTime.of(0, 30)) &&
                time.isBefore(LocalTime.of(8, 30))) ||
                (time.equals(LocalTime.of(0, 30)));
    }
}