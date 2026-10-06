package com.ms.meterpublisher.simulator;

import com.ms.meterpublisher.device.Device;
import com.ms.meterpublisher.message.MeterReading;
import com.ms.meterpublisher.reading.RawReadingRepository;
import com.ms.meterpublisher.reading.RawReadingRepository.LastReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeterSimulatorTest {

    private static final OffsetDateTime MEASURED_AT = OffsetDateTime.parse("2026-10-06T14:15:00+09:00");

    private final RawReadingRepository rawReadingRepository = mock(RawReadingRepository.class);
    private final MeterSimulator simulator = new MeterSimulator(rawReadingRepository);

    private final List<Device> devices = List.of(
            new Device(1, 1, "MAIN-A", "본관 A동 메인 수전반", "수전반", true),
            new Device(2, 1, "AHU-01", "공조기 1호", "공조", false),
            new Device(3, 1, "LIGHT-01", "본관 1층 조명", "조명", false)
    );

    @BeforeEach
    void setUp() {
        when(rawReadingRepository.findLatest(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void 이력이_없으면_0에서_시작해_1분치씩_누적한다() {
        MeterReading first = byCode(simulator.read(devices, MEASURED_AT), "AHU-01");
        MeterReading second = byCode(simulator.read(devices, MEASURED_AT.plusMinutes(1)), "AHU-01");

        assertThat(first.seq()).isEqualTo(1);
        assertThat(first.cumulativeKwh()).isEqualByComparingTo(perMinute(first.instantKw()));
        assertThat(second.seq()).isEqualTo(2);
        assertThat(second.cumulativeKwh())
                .isEqualByComparingTo(first.cumulativeKwh().add(perMinute(second.instantKw())));
    }

    @Test
    void 마지막_원본값이_있으면_그_누적값과_순번에서_이어간다() {
        when(rawReadingRepository.findLatest(2L))
                .thenReturn(Optional.of(new LastReading(new BigDecimal("1000.000"), 41)));

        MeterReading reading = byCode(simulator.read(devices, MEASURED_AT), "AHU-01");

        assertThat(reading.seq()).isEqualTo(42);
        assertThat(reading.cumulativeKwh())
                .isEqualByComparingTo(new BigDecimal("1000.000").add(perMinute(reading.instantKw())));
    }

    @Test
    void 메인_계량기는_같은_건물_하위_계량기_합보다_크다() {
        List<MeterReading> readings = simulator.read(devices, MEASURED_AT);

        BigDecimal subSum = byCode(readings, "AHU-01").instantKw()
                .add(byCode(readings, "LIGHT-01").instantKw());
        assertThat(byCode(readings, "MAIN-A").instantKw()).isGreaterThan(subSum);
    }

    private static BigDecimal perMinute(BigDecimal kw) {
        return kw.divide(BigDecimal.valueOf(60), 3, RoundingMode.HALF_UP);
    }

    private static MeterReading byCode(List<MeterReading> readings, String deviceCode) {
        return readings.stream().filter(r -> r.deviceCode().equals(deviceCode)).findFirst().orElseThrow();
    }
}
