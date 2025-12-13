package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.model.User;

@Mapper(componentModel = "spring") // автоматически регистрируется в Spring
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    User toUser(UserDto userDto);

    UserShortDto toShortDto(User user);

    UserShortDto toShortDto(UserDto userDto);
}

