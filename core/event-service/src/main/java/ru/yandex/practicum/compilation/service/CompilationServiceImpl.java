package ru.yandex.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.compilation.mapper.CompilationMapper;
import ru.yandex.practicum.compilation.model.Compilation;
import ru.yandex.practicum.compilation.repository.CompilationRepository;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.dto.compilation.UpdateCompilationRequest;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.exception.NotFoundException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional(readOnly = true)
    public Collection<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return compilationRepository.findAllByPinned(pinned, pageable).stream()
                .map(compilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = findCompilationById(compId);
        return compilationMapper.toCompilationDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Event> events = eventRepository.findAllByIdIn(newCompilationDto.getEvents());
            compilation.setEvents(events);
        } else {
            compilation.setEvents(new HashSet<>());
        }

        compilation = compilationRepository.save(compilation);
        compilationRepository.flush();
        return compilationMapper.toCompilationDto(compilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
        compilationRepository.flush();
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = findCompilationById(compId);

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllByIdIn(updateRequest.getEvents()));
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);
        compilationRepository.flush();
        return compilationMapper.toCompilationDto(compilation);
    }

    private Compilation findCompilationById(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
    }
}