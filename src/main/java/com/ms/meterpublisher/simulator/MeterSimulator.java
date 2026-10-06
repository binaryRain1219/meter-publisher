package com.ms.meterpublisher.simulator;

import com.ms.meterpublisher.device.Device;
import com.ms.meterpublisher.message.MeterReading;
import com.ms.meterpublisher.message.MeterStatus;
import com.ms.meterpublisher.message.RunState;
import com.ms.meterpublisher.reading.RawReadingRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 전력량계를 흉내 낸다. 디바이스별 누적값/순번을 메모리에 들고 1분치씩 쌓아간다.
@Component
public class MeterSimulator {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final RawReadingRepository rawReadingRepository;
    private final Map<Long, MeterState> states = new ConcurrentHashMap<>();

    public MeterSimulator(RawReadingRepository rawReadingRepository) {
        this.rawReadingRepository = rawReadingRepository;
    }

    public List<MeterReading> read(List<Device> devices, OffsetDateTime measuredAt) {
        List<MeterReading> readings = new ArrayList<>();

        // 하위 계량기를 먼저 읽고, 메인 계량기는 같은 건물 하위 계량기 합 + 기타 부하로 만든다.
        Map<Long, BigDecimal> subKwByBuilding = new HashMap<>();
        for (Device device : devices) {
            if (device.isMain()) {
                continue;
            }
            BigDecimal kw = LoadProfile.of(device.category()).randomKw();
            subKwByBuilding.merge(device.buildingId(), kw, BigDecimal::add);
            readings.add(advance(device, kw, measuredAt));
        }
        for (Device device : devices) {
            if (!device.isMain()) {
                continue;
            }
            BigDecimal kw = LoadProfile.of(device.category()).randomKw()
                    .add(subKwByBuilding.getOrDefault(device.buildingId(), BigDecimal.ZERO));
            readings.add(advance(device, kw, measuredAt));
        }
        return readings;
    }

    // 발행 성공 여부와 상관없이 계량기는 계속 쌓는다. 발행이 빠지면 seq 가 비어서 수신 쪽에서 알 수 있다.
    private MeterReading advance(Device device, BigDecimal instantKw, OffsetDateTime measuredAt) {
        MeterState state = states.computeIfAbsent(device.deviceId(), this::loadState)
                .advance(instantKw);
        states.put(device.deviceId(), state);
        return new MeterReading(
                device.deviceCode(),
                measuredAt,
                state.cumulativeKwh(),
                instantKw,
                state.seq(),
                MeterStatus.OK,
                List.of(),
                RunState.RUN
        );
    }

    // 처음 보는 디바이스는 DB 의 마지막 원본 값에서 이어가고, 없으면 0 에서 시작한다.
    private MeterState loadState(long deviceId) {
        return rawReadingRepository.findLatest(deviceId)
                .map(last -> new MeterState(last.cumulativeKwh(), last.seq()))
                .orElseGet(() -> new MeterState(BigDecimal.ZERO.setScale(3), 0));
    }

    private record MeterState(BigDecimal cumulativeKwh, long seq) {

        // 1분 동안 instantKw 로 썼다고 보고 kWh 를 더한다. DB 컬럼에 맞춰 소수 3자리로 자른다.
        MeterState advance(BigDecimal instantKw) {
            BigDecimal kwh = instantKw.divide(MINUTES_PER_HOUR, 3, RoundingMode.HALF_UP);
            return new MeterState(cumulativeKwh.add(kwh), seq + 1);
        }
    }
}
