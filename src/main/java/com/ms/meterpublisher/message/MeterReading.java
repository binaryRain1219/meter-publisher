package com.ms.meterpublisher.message;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MeterReading(
        String deviceCode,
        OffsetDateTime measuredAt,
        BigDecimal cumulativeKwh,
        BigDecimal instantKw,
        long seq,
        MeterStatus meterStatus,
        List<String> errorCodes,
        RunState runState
) {
}
