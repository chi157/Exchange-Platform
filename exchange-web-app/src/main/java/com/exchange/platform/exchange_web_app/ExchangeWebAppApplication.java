package com.exchange.platform.exchange_web_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 啟用定時任務功能
public class ExchangeWebAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExchangeWebAppApplication.class, args);
	}

}

