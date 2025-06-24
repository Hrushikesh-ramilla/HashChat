package com.hashchat.messenger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HashChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(HashChatApplication.class, args);
    }

}
