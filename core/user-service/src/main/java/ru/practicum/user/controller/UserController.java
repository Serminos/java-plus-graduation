package ru.practicum.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.user.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Запрос на создание пользователя {}", userDto);
        return userService.create(userDto);
    }

    @GetMapping
    public List<UserDto> getAllUsers(@RequestParam(required = false) List<Long> ids,
                                     @RequestParam(name = "from", defaultValue = "0") @Min(0) int from,
                                     @RequestParam(name = "size", defaultValue = "10") @Min(1) int size
    ) {
        log.info("Поиск пользователей по параметрам from {} size {}", from, size);
        return userService.getAll(ids, from, size);
    }

    @GetMapping("/{userId}")
    public UserDto getUserById(@PathVariable Long userId) {
        log.info("Поиск пользователя с id {}", userId);

        return userService.getById(userId);
    }

    @PostMapping("/batch")
    public List<UserDto> getUsersByIds(@RequestBody List<Long> userIds) {
        log.info("Поиск пользователей с id {}", userIds);
        List<UserDto> users = userService.getUserUserDtosByIds(userIds);
        return users;
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        log.info("Удаление пользователя с id {}", userId);
        userService.deleteUser(userId);
    }
}