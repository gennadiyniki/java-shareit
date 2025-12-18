package ru.practicum.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.gateway", "ru.practicum.server.dto"})
public class ShareItGatewayApplication {
public static void main(String[] args) {
       SpringApplication.run(ShareItGatewayApplication.class, args);
    }
}