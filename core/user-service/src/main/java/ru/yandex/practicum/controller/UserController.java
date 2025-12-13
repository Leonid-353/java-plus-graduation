package ru.yandex.practicum.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.feign.UserClient;
import ru.yandex.practicum.service.UserService;

import java.util.Collection;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/users")
public class UserController implements UserClient {
    private final UserService userService;

    @Override
    public UserDto createUser(UserDto userDto) {
        return userService.createUser(userDto);
    }

    @Override
    public Collection<UserDto> getUsers(Collection<Long> ids, Integer from, Integer size) {
        return userService.getUsers(ids, from, size);
    }

    @Override
    public UserDto getUserById(Long userId) {
        return userService.getUserById(userId);
    }

    @Override
    public UserShortDto getUserShortById(Long userId) {
        return userService.getUserShortById(userId);
    }

    // Получить краткую информацию о нескольких пользователях по ID
    @Override
    public List<UserShortDto> getUsersShortByIds(List<Long> userIds) {
        return userService.getUsersShortByIds(userIds);
    }

    @Override
    public void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }
}
