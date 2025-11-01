package com.exchange.platform;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class ServletConfig {
    
    @Bean
    public ServletRegistrationBean<CustomServlet> customServlet() {
        ServletRegistrationBean<CustomServlet> bean = 
            new ServletRegistrationBean<>(new CustomServlet(), "/servlet-ping");
        bean.setLoadOnStartup(1);
        return bean;
    }
    
    public static class CustomServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws IOException {
            resp.setContentType("text/plain");
            resp.getWriter().println("SERVLET-PONG");
        }
    }
}