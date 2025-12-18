package ru.practicum.server.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ItemRequestDto;
import ru.practicum.dto.request.ItemRequestResponseDto;

import java.util.List;

/**
 * TODO Sprint add-item-requests.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/requests")
public class ItemRequestController {

    private final ItemRequestService itemRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemRequestResponseDto createItemRequest(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId,
            @Valid @RequestBody ItemRequestDto itemRequestDto) {
        log.info("POST /requests - Создание запроса пользователем {}", userId);
        return itemRequestService.createRequest(userId, itemRequestDto);
    }

    @GetMapping
    public List<ItemRequestResponseDto> getUserItemRequests(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /requests - Получение запросов пользователя {}", userId);
        return itemRequestService.getUserRequests(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestResponseDto> getAllItemRequests(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("GET /requests/all - Получение всех запросов, from={}, size={}", from, size);
        return itemRequestService.getAllRequests(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ItemRequestResponseDto getItemRequestById(
            @PathVariable @Positive Long requestId,
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /requests/{} - Получение запроса по ID пользователем {}", requestId, userId);
        return itemRequestService.getRequestById(userId, requestId);
    }
}