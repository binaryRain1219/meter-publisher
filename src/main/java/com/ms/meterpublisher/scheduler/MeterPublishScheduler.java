package com.ms.meterpublisher.scheduler;

import com.ms.meterpublisher.device.Device;
import com.ms.meterpublisher.device.DeviceRepository;
import com.ms.meterpublisher.message.MeterReading;
import com.ms.meterpublisher.mqtt.MeterReadingPublisher;
import com.ms.meterpublisher.simulator.MeterSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

// 매분 0초에 등록된 장비 전체의 측정값을 만들어 발행한다.
@Component
public class MeterPublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(MeterPublishScheduler.class);

    // 서버 시간대(EC2 는 UTC)와 상관없이 한국 시각으로 측정 시각을 찍는다.
    private static final String ZONE = "Asia/Seoul";

    private final DeviceRepository deviceRepository;
    private final MeterSimulator meterSimulator;
    private final MeterReadingPublisher publisher;

    public MeterPublishScheduler(DeviceRepository deviceRepository,
                                 MeterSimulator meterSimulator,
                                 MeterReadingPublisher publisher) {
        this.deviceRepository = deviceRepository;
        this.meterSimulator = meterSimulator;
        this.publisher = publisher;
    }

    @Scheduled(cron = "0 * * * * *", zone = ZONE)
    public void publishReadings() {
        // 스케줄러가 몇 ms 늦게 깨어나도 측정 시각은 정확히 분 단위로 맞춘다.
        OffsetDateTime measuredAt = OffsetDateTime.now(ZoneId.of(ZONE)).truncatedTo(ChronoUnit.MINUTES);

        List<Device> devices = deviceRepository.findAll();
        List<MeterReading> readings = meterSimulator.read(devices, measuredAt);
        int published = publisher.publish(readings);

        log.info("측정값 발행: measuredAt={}, 장비 {}대, 발행 {}건", measuredAt, devices.size(), published);
    }
}
