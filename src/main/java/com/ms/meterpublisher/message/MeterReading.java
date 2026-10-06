package com.ms.meterpublisher.message;

import java.time.LocalDateTime;

public record MeterReading(
        String deviceId,
        double cumulativeKwh,
        LocalDateTime measuerdAt
) {
}
