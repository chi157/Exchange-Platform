package com.exchange.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ExchangeWebAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExchangeWebAppApplication.class, args);
    }
}

@RestController
class HelloController {
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
