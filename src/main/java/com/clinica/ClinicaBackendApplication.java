package com.clinica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.clinica"})
public class ClinicaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicaBackendApplication.class, args);
    }

}