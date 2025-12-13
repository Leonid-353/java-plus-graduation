package ru.yandex.practicum.feign;

import feign.FeignException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.stats.StatsDtoRequest;
import ru.yandex.practicum.dto.stats.StatsDtoResponse;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsClient {

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    void hit(@Valid @RequestBody StatsDtoRequest request) throws FeignException;

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    List<StatsDtoResponse> findStats(
            @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @NotNull LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @NotNull LocalDateTime end,
            @RequestParam(name = "uris", required = false) List<String> uris,
            @RequestParam(name = "unique", defaultValue = "false") Boolean unique) throws FeignException;
}