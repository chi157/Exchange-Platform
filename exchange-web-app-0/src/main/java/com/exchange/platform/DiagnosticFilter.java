package com.exchange.platform;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Configuration
public class DiagnosticFilter {
    
    @Bean
    public FilterRegistrationBean<RequestDiagnosticFilter> diagnosticFilter() {
        FilterRegistrationBean<RequestDiagnosticFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestDiagnosticFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
    
    public static class RequestDiagnosticFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            System.out.println("=== FILTER DIAGNOSTIC ===");
            System.out.println("Request URI: " + httpRequest.getRequestURI());
            System.out.println("Context Path: " + httpRequest.getContextPath());
            System.out.println("Servlet Path: " + httpRequest.getServletPath());
            System.out.println("Path Info: " + httpRequest.getPathInfo());
            System.out.println("=== END FILTER DIAGNOSTIC ===");
            
            chain.doFilter(request, response);
        }
    }
}