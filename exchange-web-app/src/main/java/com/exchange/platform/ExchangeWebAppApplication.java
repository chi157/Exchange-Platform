package com.exchange.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ExchangeWebAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExchangeWebAppApplication.class, args);
    }
}
