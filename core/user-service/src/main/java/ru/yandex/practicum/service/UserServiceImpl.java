package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.mapper.UserMapper;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.UserRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    final UserRepository userRepository;
    final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Creating user with name {}, email {}", userDto.getName(), userDto.getEmail());
        User user = userMapper.toUser(userDto);
        user = userRepository.save(user);
        userRepository.flush();
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserDto> getUsers(Collection<Long> ids, Integer from, Integer size) {
        Collection<UserDto> userDtos;
        log.info("Getting users with ids {}, from {}, size {}", ids, from, size);
        if (size.equals(0)) return List.of();
        Pageable pageable = PageRequest.of(from / size, size);
        if (ids == null || ids.isEmpty()) {
            userDtos = userRepository.findAll(pageable).stream().map(userMapper::toDto).toList();
        } else {
            userDtos = userRepository.findUsersByIds(ids, pageable).stream().map(userMapper::toDto).toList();
        }
        return userDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        return userMapper.toDto(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Not found user with ID: %s", userId))));
    }

    @Override
    @Transactional(readOnly = true)
    public UserShortDto getUserShortById(Long userId) {
        return userMapper.toShortDto(getUserById(userId));
    }

    // Получить краткую информацию о нескольких пользователях по ID
    @Override
    @Transactional(readOnly = true)
    public List<UserShortDto> getUsersShortByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.findByIdIn(userIds);

        return users.stream()
                .map(userMapper::toShortDto)
                .collect(Collectors.toList());

    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Attempting to delete user with id={}", userId);
        userRepository.delete(userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with id={} not found", userId);
                    return new NotFoundException(String.format("User with id=%d was not found", userId));
                }));
        userRepository.flush();
        log.info("User with id={} successfully deleted", userId);
    }
}
