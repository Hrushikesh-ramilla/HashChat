package com.hashchat.messenger.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hashchat.demo.enabled", havingValue = "true")
public class DemoController {

    private final DemoService demoService;

    @GetMapping("/seed")
    public Map<String, Object> seed() {
        return demoService.seed();
    }
}
