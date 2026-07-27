package com.inklusport.sports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Este microservicio funciona */

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class SportsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportsApplication.class, args);
    }
}