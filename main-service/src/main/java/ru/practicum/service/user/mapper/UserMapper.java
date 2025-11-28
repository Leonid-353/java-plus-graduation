package ru.practicum.service.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.service.user.dto.UserDto;
import ru.practicum.service.user.model.User;

@Mapper(componentModel = "spring") // автоматически регистрируется в Spring
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    User toUser(UserDto userDto);
}

