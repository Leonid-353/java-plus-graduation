package ru.practicum.service.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ru.practicum.service.stats.client.config.FeignConfig.class)
public class StatsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatsServiceApplication.class, args);
    }
}
