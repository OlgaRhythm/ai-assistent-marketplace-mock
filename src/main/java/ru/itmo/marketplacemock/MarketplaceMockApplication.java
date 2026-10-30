package ru.itmo.marketplacemock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MarketplaceMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketplaceMockApplication.class, args);
    }
}
