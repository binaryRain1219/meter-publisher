package com.ms.meterpublisher.mqtt;

import com.ms.meterpublisher.message.MeterReading;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

// MeterReading 을 JSON 으로 바꿔 장비별 토픽에 발행한다.
@Component
public class MeterReadingPublisher {

    private static final Logger log = LoggerFactory.getLogger(MeterReadingPublisher.class);

    private final MqttProperties properties;
    private final JsonMapper jsonMapper;
    private final MqttClient client;
    private final MqttConnectOptions connectOptions;

    public MeterReadingPublisher(MqttProperties properties, JsonMapper jsonMapper) throws MqttException {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        // 기본 생성자는 작업 디렉터리에 상태 파일을 만들기 때문에 메모리 저장소를 쓴다.
        this.client = new MqttClient(properties.brokerUrl(), properties.clientId(), new MemoryPersistence());
        this.client.setTimeToWait(10_000);
        this.connectOptions = connectOptions(properties);
    }

    // 발행한 건수를 돌려준다. 브로커에 붙지 못하면 이번 주기는 건너뛰고 다음 주기에 다시 연결한다.
    public synchronized int publish(List<MeterReading> readings) {
        if (!ensureConnected()) {
            return 0;
        }
        int published = 0;
        for (MeterReading reading : readings) {
            try {
                client.publish(topicOf(reading), jsonMapper.writeValueAsBytes(reading), properties.qos(), false);
                published++;
            } catch (MqttException e) {
                log.warn("MQTT 발행 실패: deviceCode={}, seq={} ({})", reading.deviceCode(), reading.seq(), e.getMessage());
            }
        }
        return published;
    }

    private boolean ensureConnected() {
        if (client.isConnected()) {
            return true;
        }
        try {
            client.connect(connectOptions);
            log.info("MQTT 브로커 연결: {}", properties.brokerUrl());
            return true;
        } catch (MqttException e) {
            log.warn("MQTT 브로커 연결 실패: {} ({})", properties.brokerUrl(), e.getMessage());
            return false;
        }
    }

    private String topicOf(MeterReading reading) {
        return properties.topicPattern().replace("{deviceCode}", reading.deviceCode());
    }

    @PreDestroy
    synchronized void close() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
    }

    private static MqttConnectOptions connectOptions(MqttProperties properties) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        if (properties.username() != null && !properties.username().isBlank()) {
            options.setUserName(properties.username());
            options.setPassword(Objects.requireNonNullElse(properties.password(), "").toCharArray());
        }
        return options;
    }
}
