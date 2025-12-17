package ru.yandex.practicum.feign;

import feign.FeignException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users")
public interface UserClient {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserDto createUser(@Valid @RequestBody UserDto userDto) throws FeignException;

    @GetMapping
    Collection<UserDto> getUsers(@RequestParam(required = false) Collection<Long> ids,
                                 @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                 @Positive @RequestParam(defaultValue = "10") Integer size) throws FeignException;

    @GetMapping("/{userId}")
    UserDto getUserById(@PathVariable Long userId) throws FeignException;

    // Получить краткую информацию о пользователе по ID
    @GetMapping("/{userId}/short")
    UserShortDto getUserShortById(@PathVariable Long userId) throws FeignException;

    // Получить краткую информацию о нескольких пользователях по ID
    @PostMapping("/short")
    List<UserShortDto> getUsersShortByIds(@RequestBody List<Long> userIds) throws FeignException;

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteUser(@PathVariable Long userId) throws FeignException;
}
