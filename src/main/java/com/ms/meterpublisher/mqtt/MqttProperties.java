package com.ms.meterpublisher.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public record MqttProperties(
        String brokerUrl,
        String clientId,
        String topicPattern,  // {deviceCode} 자리에 장비 코드가 들어간다
        int qos,
        String username,      // 브로커 인증을 켠 경우에만 설정
        String password
) {
}
