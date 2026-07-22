package com.bankagn.bankagn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BankaGnApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankaGnApplication.class, args);
    }
}