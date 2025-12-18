package ru.practicum.server.booking;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.booking.BookingDto;
import ru.practicum.dto.booking.BookingResponseDto;
import ru.practicum.dto.booking.BookingState;
import ru.practicum.server.exception.ValidationException;

import java.util.List;

@Validated
@RestController
@RequestMapping("/bookings")
public class BookingController {
    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    //Создание бронирования POST /bookings
    @PostMapping
    public BookingResponseDto createBooking(  // ← BookingResponseDto, не BookingDto!
                                              @RequestHeader("X-Sharer-User-Id") Long bookerId,
                                              @Valid @RequestBody BookingDto bookingDto) {
        log.info("POST /bookings - Запрос на создание бронирования пользователем {}: {}", bookerId, bookingDto);
        BookingResponseDto result = bookingService.createBooking(bookerId, bookingDto);
        log.info("POST /bookings - Бронирование успешно создано: {}", result);
        return result;
    }

    @PatchMapping("/{bookingId}")
    public BookingResponseDto bookingStatusUpdate(@PathVariable Long bookingId,  // ← BookingResponseDto
                                                  @RequestParam Boolean approved,
                                                  @RequestHeader("X-Sharer-User-Id") Long bookerId) {
        log.info("PATCH /bookings/{} - Запрос на изменение статуса бронирования: approved={}, userId={}",
                bookingId, approved, bookerId);
        BookingResponseDto result = bookingService.bookingStatusUpdate(bookingId, approved, bookerId);
        log.info("PATCH /bookings/{} - Статус бронирования успешно изменен: {}", bookingId, result);
        return result;
    }

    @GetMapping("/{bookingId}")
    public BookingResponseDto getBookingById(@PathVariable Long bookingId,  // ← BookingResponseDto
                                             @RequestHeader("X-Sharer-User-Id") Long bookerId) {
        log.info("GET /bookings/{} - Запрос на получение бронирования пользователем {}", bookingId, bookerId);
        BookingResponseDto result = bookingService.getBookingById(bookingId, bookerId);
        log.info("GET /bookings/{} - Бронирование получено: {}", bookingId, result);
        return result;
    }

    @GetMapping
    public List<BookingResponseDto> getUserBookings(@RequestHeader("X-Sharer-User-Id") Long bookerId,  // ← List<BookingResponseDto>
                                                    @RequestParam(defaultValue = "ALL") String state,
                                                    @RequestParam(defaultValue = "0") int from,
                                                    @RequestParam(defaultValue = "10") int size) {
        log.info("GET /bookings - Запрос списка бронирований пользователя: userId={}, state={}, from={}, size={}",
                bookerId, state, from, size);
        BookingState bookingState = BookingState.valueOf(state.toUpperCase());

        List<BookingResponseDto> result = bookingService.getUserBookings(bookerId, bookingState, from, size);
        log.info("GET /bookings - Найдено {} бронирований для пользователя {}", result.size(), bookerId);
        return result;
    }

    @GetMapping("/owner")
    public List<BookingResponseDto> getOwnerBookings(  // ← List<BookingResponseDto>
                                                       @RequestHeader("X-Sharer-User-Id") Long userId,
                                                       @RequestParam(defaultValue = "ALL") String state,
                                                       @RequestParam(defaultValue = "0") int from,
                                                       @RequestParam(defaultValue = "10") int size) {
        log.info("GET /bookings/owner - Запрос списка бронирований владельца: ownerId={}, state={}, from={}, size={}",
                userId, state, from, size);
        BookingState bookingState = BookingState.valueOf(state.toUpperCase());
        List<BookingResponseDto> result = bookingService.getOwnerBookings(userId, bookingState, from, size);
        log.info("GET /bookings/owner - Найдено {} бронирований для владельца {}", result.size(), userId);
        return result;
    }
}