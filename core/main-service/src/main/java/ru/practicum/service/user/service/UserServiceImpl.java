package ru.practicum.service.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.service.exception.NotFoundException;
import ru.practicum.service.user.dto.UserDto;
import ru.practicum.service.user.mapper.UserMapper;
import ru.practicum.service.user.model.User;
import ru.practicum.service.user.repository.UserRepository;

import java.util.Collection;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    final UserRepository userRepository;
    final UserMapper userMapper;

    @Override
    public UserDto createUser(UserDto userDto) {
        log.info("Creating user with name {}, email {}", userDto.getName(), userDto.getEmail());
        User user = userMapper.toUser(userDto);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Override
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
    public void deleteUser(Long userId) {
        log.info("Attempting to delete user with id={}", userId);
        userRepository.delete(userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with id={} not found", userId);
                    return new NotFoundException(String.format("User with id=%d was not found", userId));
                }));
        log.info("User with id={} successfully deleted", userId);
    }
}
