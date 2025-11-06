package com.exchange.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
                        .allowedHeaders("*");
            }
            
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // 配置圖片靜態資源映射
                // 使用絕對路徑 file: 協議，確保 JAR 執行時也能找到圖片
                registry.addResourceHandler("/images/**")
                        .addResourceLocations("file:uploads/images/")
                        .setCachePeriod(3600); // 快取 1 小時
            }
        };
    }
}
