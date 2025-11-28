package ru.practicum.service.user.service;

import ru.practicum.service.user.dto.UserDto;

import java.util.Collection;

public interface UserService {

    UserDto createUser(UserDto userDto);

    Collection<UserDto> getUsers(Collection<Long> ids, Integer from, Integer size);

    void deleteUser(Long id);
}
