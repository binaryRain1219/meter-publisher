package com.ms.meterpublisher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeterPublisherApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeterPublisherApplication.class, args);
    }

}
