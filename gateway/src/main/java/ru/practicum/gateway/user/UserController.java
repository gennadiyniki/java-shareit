package ru.practicum.gateway.user;

import ru.practicum.dto.user.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserClient userClient;

    @PostMapping
    public ResponseEntity<Object> createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Gateway: createUser request {}", userDto);
        ResponseEntity<Object> response = userClient.createUser(userDto);
        return fixResponseContentType(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Object> getUser(@PathVariable Long userId) {
        ResponseEntity<Object> error = validateId(userId);
        if (error != null) return error;

        log.info("Gateway: getUser request id={}", userId);
        ResponseEntity<Object> response = userClient.getUser(userId);
        return fixResponseContentType(response);
    }

    @GetMapping
    public ResponseEntity<Object> getAllUsers() {
        log.info("Gateway: getAllUsers request");
        ResponseEntity<Object> response = userClient.getAllUsers();
        return fixResponseContentType(response);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<Object> updateUser(@PathVariable Long userId,
                                             @RequestBody UserDto userDto) {
        ResponseEntity<Object> error = validateId(userId);
        if (error != null) return error;

        log.info("Gateway: updateUser request id={}, dto={}", userId, userDto);
        ResponseEntity<Object> response = userClient.updateUser(userId, userDto);
        return fixResponseContentType(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Object> deleteUser(@PathVariable Long userId) {
        ResponseEntity<Object> error = validateId(userId);
        if (error != null) return error;

        log.info("Gateway: deleteUser request id={}", userId);
        ResponseEntity<Object> response = userClient.deleteUser(userId);
        return fixResponseContentType(response);
    }

    private ResponseEntity<Object> validateId(Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Id must be positive");
        }
        return null;
    }

    /**
     * Исправляет Content-Type в ответе, если он application/octet-stream
     * и устанавливает правильный application/json
     */
    private ResponseEntity<Object> fixResponseContentType(ResponseEntity<Object> response) {
        if (response == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error");
        }

        // Если статус ошибки (4xx или 5xx) и Content-Type octet-stream
        if ((response.getStatusCode().is4xxClientError() ||
                response.getStatusCode().is5xxServerError()) &&
                response.getHeaders().getContentType() != null &&
                response.getHeaders().getContentType().includes(MediaType.APPLICATION_OCTET_STREAM)) {

            HttpHeaders headers = new HttpHeaders();
            headers.putAll(response.getHeaders());
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(
                    response.getBody(),
                    headers,
                    response.getStatusCode()
            );
        }

        // Для успешных ответов или правильных Content-Type возвращаем как есть
        return response;
    }
}