package com.futureboundtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FutureBoundTechApplication {

    public static void main(String[] args) {
        SpringApplication.run(FutureBoundTechApplication.class, args);
    }
}
