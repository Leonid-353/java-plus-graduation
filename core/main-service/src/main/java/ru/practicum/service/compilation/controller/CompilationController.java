package ru.practicum.service.compilation.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.compilation.dto.CompilationDto;
import ru.practicum.service.compilation.service.CompilationService;

import java.util.Collection;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Slf4j
public class CompilationController {
    private final CompilationService compilationService;

    @GetMapping
    public Collection<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        log.info("Получен запрос GET /compilations с параметрами: pinned={}, from={}, size={}", pinned, from, size);

        return compilationService.getCompilations(pinned, from, size);
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilation(@PathVariable Long compId) {
        log.info("Получен запрос GET /compilations/{}", compId);

        return compilationService.getCompilation(compId);
    }
}