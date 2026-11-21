package com.devjuice.flagman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // StreamHub pings open streams
public class FlagManApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlagManApplication.class, args);
    }
    

}
