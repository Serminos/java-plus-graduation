package ru.practicum.user.service;


import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(UserDto userDto);

    List<UserDto> getAll(List<Long> ids, int from, int size);

    UserDto getById(Long id);

    List<UserDto> getUserUserDtosByIds(List<Long> ids);

    void deleteUser(Long userId);
}
