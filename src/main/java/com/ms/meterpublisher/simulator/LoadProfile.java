package com.ms.meterpublisher.simulator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

// 디바이스 종류별 순시전력(kW) 범위.
record LoadProfile(double minKw, double maxKw) {

    static LoadProfile of(String category) {
        return switch (category) {
            case "수전반" -> new LoadProfile(20, 50);  // 메인 계량기: 하위 계량기 합에 더하는 기타 부하
            case "공조" -> new LoadProfile(15, 40);
            case "조명" -> new LoadProfile(4, 8);
            default -> new LoadProfile(1, 5);
        };
    }

    BigDecimal randomKw() {
        double kw = ThreadLocalRandom.current().nextDouble(minKw, maxKw);
        return BigDecimal.valueOf(kw).setScale(2, RoundingMode.HALF_UP);
    }
}
