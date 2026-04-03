package com.bookie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookieApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookieApplication.class, args);
    }
}