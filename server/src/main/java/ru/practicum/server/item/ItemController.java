package ru.practicum.server.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.item.CommentDto;
import ru.practicum.dto.item.ItemDto;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/items")
public class ItemController {
    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    private final ItemService itemService;

    @Autowired
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    // создание вещи
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto createItemDto(
            @RequestHeader("X-Sharer-User-Id") Long ownerId,
            @RequestBody ItemDto itemDto) {
        log.info("POST /items - Запрос на создание вещи пользователем {}: {}", ownerId, itemDto);
        return itemService.createItem(ownerId, itemDto);
    }

    // просмотр вещи по id
    @GetMapping("/{itemId}")
    public ItemDto getItemById(@PathVariable Long itemId) {  // ← НЕТ @Positive
        log.info("GET /items/{} - Запрос на просмотр информации вещи с id: {}", itemId, itemId);
        return itemService.getItemById(itemId);
    }

    // просмотр вещй владельца
    @GetMapping()
    public Collection<ItemDto> getMyItems(@RequestHeader("X-Sharer-User-Id") Long ownerId) {  // ← НЕТ @Positive
        log.info("GET /items - Запрос на вывод вещей владельца: {}", ownerId);
        return itemService.getItemsByOwner(ownerId);
    }

    // поиск вещи по названию или описанию
    @GetMapping("/search")
    public Collection<ItemDto> searchItem(@RequestParam String text) {
        log.info("GET /items/search - Запрос на поиск вещей по тексту: '{}'", text);

        if (text.isEmpty()) {
            return List.of();
        }
        return itemService.searchAvailableItems(text);
    }

    // обновление вещи пользователем
    @PatchMapping("/{itemId}")
    public ItemDto updateItem(
            @PathVariable Long itemId,
            @RequestBody ItemDto itemDto,  // ← БЕЗ аннотаций
            @RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("PATCH /items/{} - Запрос на обновление вещи пользователем {}", itemId, userId);
        return itemService.updateItem(itemId, itemDto, userId);
    }

    // вывод всех вещей
    @GetMapping("/allTeam")
    public Collection<ItemDto> getAllItems() {
        log.info("GET /items - Запрос на вывод всех вещей");
        return itemService.getAllItems();
    }

    // добавление комментария
    @PostMapping("/{itemId}/comment")
    public CommentDto addComment(@PathVariable Long itemId,
                                 @RequestHeader("X-Sharer-User-Id") Long userId,
                                 @RequestBody CommentDto commentDto) {  // ← НЕТ @Valid
        log.info("POST /items/{}/comment - Запрос на добавление комментария пользователем {}", itemId, userId);
        return itemService.addComment(userId, itemId, commentDto);
    }
}