package com.ms.meterpublisher.scheduler;

import com.ms.meterpublisher.device.Device;
import com.ms.meterpublisher.device.DeviceRepository;
import com.ms.meterpublisher.message.MeterReading;
import com.ms.meterpublisher.message.MeterStatus;
import com.ms.meterpublisher.message.RunState;
import com.ms.meterpublisher.mqtt.MeterReadingPublisher;
import com.ms.meterpublisher.simulator.MeterSimulator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeterPublishSchedulerTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final MeterSimulator meterSimulator = mock(MeterSimulator.class);
    private final MeterReadingPublisher publisher = mock(MeterReadingPublisher.class);
    private final MeterPublishScheduler scheduler =
            new MeterPublishScheduler(deviceRepository, meterSimulator, publisher);

    @Test
    void 장비를_읽어_한국시각_분단위로_측정값을_만들고_발행한다() {
        List<Device> devices = List.of(new Device(2, 1, "AHU-01", "공조기 1호", "공조", false));
        List<MeterReading> readings = List.of(new MeterReading(
                "AHU-01", OffsetDateTime.parse("2026-10-06T14:15:00+09:00"),
                new BigDecimal("0.500"), new BigDecimal("30.00"), 1, MeterStatus.OK, List.of(), RunState.RUN));
        when(deviceRepository.findAll()).thenReturn(devices);
        when(meterSimulator.read(eq(devices), any())).thenReturn(readings);

        scheduler.publishReadings();

        ArgumentCaptor<OffsetDateTime> measuredAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(meterSimulator).read(eq(devices), measuredAt.capture());
        assertThat(measuredAt.getValue().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(measuredAt.getValue().getSecond()).isZero();
        assertThat(measuredAt.getValue().getNano()).isZero();
        verify(publisher).publish(readings);
    }
}
