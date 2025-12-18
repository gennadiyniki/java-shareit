package ru.practicum.gateway.user;

import ru.practicum.dto.user.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserClient userClient;

    @PostMapping
    public Object createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Gateway: createUser request {}", userDto);
        return userClient.createUser(userDto).getBody();
    }

    @GetMapping("/{userId}")
    public Object getUser(@PathVariable Long userId) {
        ResponseEntity<Object> error = validateId(userId);
        if (error != null) return error.getBody();

        log.info("Gateway: getUser request id={}", userId);
        return userClient.getUser(userId).getBody();
    }

    @GetMapping
    public Object getAllUsers() {
        log.info("Gateway: getAllUsers request");
        return userClient.getAllUsers().getBody();
    }

    @PatchMapping("/{userId}")
    public Object updateUser(@PathVariable Long userId,
                             @RequestBody UserDto userDto) {
        ResponseEntity<Object> error = validateId(userId);
        if (error != null) return error.getBody();

        log.info("Gateway: updateUser request id={}, dto={}", userId, userDto);
        return userClient.updateUser(userId, userDto).getBody();
    }

    @DeleteMapping("/{userId}")
    public Object deleteUser(@PathVariable Long userId) {
        ResponseEntity<Object> error = validateId(userId);
        if (error != null) return error.getBody();

        log.info("Gateway: deleteUser request id={}", userId);
        return userClient.deleteUser(userId).getBody();
    }

    private ResponseEntity<Object> validateId(Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Id must be positive");
        }
        return null;
    }
}