package com.bgpay.bgai.utils;

import java.time.*;

public class TimeZoneUtils {
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

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