package com.ms.meterpublisher.device;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 로컬 MySQL(docker compose) 에 02-seed.sql 이 들어가 있어야 한다.
@SpringBootTest
class DeviceRepositoryTest {

    @Autowired
    DeviceRepository deviceRepository;

    @Test
    void findAll_시드로_넣은_디바이스를_모두_읽는다() {
        List<Device> devices = deviceRepository.findAll();

        assertThat(devices)
                .extracting(Device::deviceCode)
                .contains("MAIN-A", "AHU-01", "LIGHT-01");
        assertThat(devices)
                .filteredOn(d -> d.deviceCode().equals("MAIN-A"))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.category()).isEqualTo("수전반");
                    assertThat(d.isMain()).isTrue();
                });
    }
}
