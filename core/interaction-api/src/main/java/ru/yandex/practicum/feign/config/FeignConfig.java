package ru.yandex.practicum.feign.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "ru.yandex.practicum.feign")
public class FeignConfig {
}
