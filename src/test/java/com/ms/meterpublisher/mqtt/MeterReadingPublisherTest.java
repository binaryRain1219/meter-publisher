package com.ms.meterpublisher.mqtt;

import com.ms.meterpublisher.message.MeterReading;
import com.ms.meterpublisher.message.MeterStatus;
import com.ms.meterpublisher.message.RunState;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 로컬 ActiveMQ(docker compose) 가 떠 있어야 한다.
// 실제 토픽(meter/...)에 테스트 값이 섞이지 않도록 test/ 로 시작하는 토픽을 쓴다.
@SpringBootTest
class MeterReadingPublisherTest {

    private static final String TEST_TOPIC_PATTERN = "test/meter/{deviceCode}/energy";

    @Autowired
    MqttProperties properties;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void 장비별_토픽으로_JSON_메시지를_발행한다() throws Exception {
        MeterReadingPublisher publisher = publisherFor(properties.brokerUrl());
        CompletableFuture<Received> received = new CompletableFuture<>();
        MqttClient subscriber = new MqttClient(properties.brokerUrl(), "meter-publisher-test-sub", new MemoryPersistence());
        subscriber.connect();
        subscriber.subscribe("test/meter/+/energy", 1, (topic, message) ->
                received.complete(new Received(topic, new String(message.getPayload(), StandardCharsets.UTF_8))));
        try {
            assertThat(publisher.publish(List.of(reading()))).isEqualTo(1);

            Received message = received.get(5, TimeUnit.SECONDS);
            assertThat(message.topic()).isEqualTo("test/meter/AHU-01/energy");
            JSONAssert.assertEquals("""
                    {
                      "deviceCode": "AHU-01",
                      "measuredAt": "2026-10-06T14:15:00+09:00",
                      "cumulativeKwh": 1234.500,
                      "instantKw": 12.30,
                      "seq": 42,
                      "meterStatus": "OK",
                      "errorCodes": [],
                      "runState": "RUN"
                    }
                    """, message.payload(), JSONCompareMode.STRICT);
        } finally {
            subscriber.disconnect();
            subscriber.close();
            publisher.close();
        }
    }

    @Test
    void 브로커에_연결하지_못하면_발행하지_않고_0을_돌려준다() throws Exception {
        MeterReadingPublisher publisher = publisherFor("tcp://localhost:1");
        try {
            assertThat(publisher.publish(List.of(reading()))).isZero();
        } finally {
            publisher.close();
        }
    }

    private MeterReadingPublisher publisherFor(String brokerUrl) throws MqttException {
        MqttProperties testProperties = new MqttProperties(
                brokerUrl, "meter-publisher-test-pub", TEST_TOPIC_PATTERN, 1, null, null);
        return new MeterReadingPublisher(testProperties, jsonMapper);
    }

    private static MeterReading reading() {
        return new MeterReading(
                "AHU-01",
                OffsetDateTime.parse("2026-10-06T14:15:00+09:00"),
                new BigDecimal("1234.500"),
                new BigDecimal("12.30"),
                42,
                MeterStatus.OK,
                List.of(),
                RunState.RUN
        );
    }

    record Received(String topic, String payload) {
    }
}
