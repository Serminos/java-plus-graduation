package ru.practicum.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users", fallbackFactory = UserApiFallback.class)
public interface UserApi {
    @PostMapping
    UserDto createUser(@Valid @RequestBody UserDto userDto);

    @GetMapping
    List<UserDto> getAllUsers(@RequestParam(required = false) List<Long> ids,
            @RequestParam(name = "from", defaultValue = "0") @Min(0) int from,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size
    );

    @GetMapping("/{userId}")
    UserDto getUserById(@PathVariable Long userId);

    @GetMapping("/batch")
    List<UserDto> getUsersByIds(@RequestBody List<Long> userIds);

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteUser(@PathVariable Long userId);
}
