package com.exchange.platform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/ping123")
    public String ping() {
        return "pong";
    }
}
