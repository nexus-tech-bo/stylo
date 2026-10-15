package com.example.styloflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Auth is JWT only: no in-memory default user (and no generated password in the logs).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class StyloflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(StyloflowApplication.class, args);
    }

}
