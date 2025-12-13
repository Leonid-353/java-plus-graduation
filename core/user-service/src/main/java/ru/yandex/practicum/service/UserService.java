package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;

import java.util.Collection;
import java.util.List;

public interface UserService {

    UserDto createUser(UserDto userDto);

    Collection<UserDto> getUsers(Collection<Long> ids, Integer from, Integer size);

    UserDto getUserById(Long userId);

    UserShortDto getUserShortById(Long userId);

    List<UserShortDto> getUsersShortByIds(List<Long> userIds);

    void deleteUser(Long id);
}
