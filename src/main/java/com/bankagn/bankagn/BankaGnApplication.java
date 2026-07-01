package com.bankagn.bankagn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class BankaGnApplication {

    public static void main(String[] args) {
        // Afficher le hash de admin123
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("HASH = " + encoder.encode("admin123"));

        SpringApplication.run(BankaGnApplication.class, args);
    }
}