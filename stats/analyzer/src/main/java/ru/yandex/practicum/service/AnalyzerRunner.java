package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    final UserActionProcessor userActionProcessor;
    final EventSimilarityProcessor eventSimilarityProcessor;

    @Override
    public void run(String... args) {
        Thread userActionThread = new Thread(userActionProcessor);
        userActionThread.setName("UserActionHandlerThread");
        userActionThread.start();

        eventSimilarityProcessor.start();
    }
}
