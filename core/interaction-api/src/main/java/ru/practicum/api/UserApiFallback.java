package ru.practicum.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ServiceUnavailableException;

import java.util.List;

@Component
@Slf4j
public class UserApiFallback {

    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        log.warn("Активирован резервный вариант для createUser для пользователя {} ", userDto);
        throw new ServiceUnavailableException("UserService недоступен");
    }

    List<UserDto> getAllUsers(
            @RequestParam(name = "from", defaultValue = "0") @Min(0) int from,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size
    ) {
        log.warn("Активирован резервный вариант для getAllUsers с параметрами from {} size", from, size);
        throw new ServiceUnavailableException("UserService недоступен");
    }

    public UserDto getUserById(@RequestParam Long ids) {
        log.warn("Активирован резервный вариант для getUserById для пользователей с id {} ", ids);
        throw new ServiceUnavailableException("UserService недоступен");
    }

    public void deleteUser(@PathVariable Long userId) {
        log.warn("Активирован резервный вариант для deleteUser для пользователей с id {} ", userId);
        throw new ServiceUnavailableException("UserService недоступен");
    }
}
