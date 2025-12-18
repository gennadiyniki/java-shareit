package ru.practicum.server.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.booking.BookingDto;
import ru.practicum.dto.booking.BookingResponseDto;
import ru.practicum.dto.booking.BookingState;
import ru.practicum.dto.booking.BookingStatus;
import ru.practicum.server.exception.AccessDeniedException;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.exception.ValidationException;
import ru.practicum.server.item.Item;
import ru.practicum.server.item.ItemRepository;
import ru.practicum.server.user.User;
import ru.practicum.server.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponseDto createBooking(Long bookerId, BookingDto bookingDto) {
        log.info("=== НАЧАЛО СОЗДАНИЯ БРОНИРОВАНИЯ ===");
        log.info("bookerId: {}, bookingDto: itemId={}, start={}, end={}",
                bookerId, bookingDto.getItemId(), bookingDto.getStart(), bookingDto.getEnd());

        try {
            // 1. Проверяем существование пользователя
            log.debug("Поиск пользователя с id={}", bookerId);
            User booker = userRepository.findById(bookerId)
                    .orElseThrow(() -> {
                        log.error("❌ Пользователь не найден: bookerId={}", bookerId);
                        return new NotFoundException("Пользователь не найден");
                    });
            log.info("✅ Пользователь найден: id={}, name={}", booker.getId(), booker.getName());

            // 2. Проверяем существование вещи
            log.debug("Поиск вещи с id={}", bookingDto.getItemId());
            Item item = itemRepository.findById(bookingDto.getItemId())
                    .orElseThrow(() -> {
                        log.error("❌ Вещь не найдена: itemId={}", bookingDto.getItemId());
                        return new NotFoundException("Вещь не найдена");
                    });
            log.info("✅ Вещь найдена: id={}, name={}, ownerId={}, available={}",
                    item.getId(), item.getName(), item.getOwner().getId(), item.getAvailable());

            // 3. Проверяем, что пользователь не владелец
            if (item.getOwner().getId().equals(bookerId)) {
                log.error("❌ Владелец пытается забронировать свою вещь: ownerId={}, bookerId={}",
                        item.getOwner().getId(), bookerId);
                throw new AccessDeniedException("Владелец не может бронировать свою вещь");
            }

            // 4. Проверяем доступность вещи
            if (!item.getAvailable()) {
                log.error("❌ Вещь недоступна для бронирования: itemId={}, available={}",
                        item.getId(), item.getAvailable());
                throw new ValidationException("Вещь недоступна для бронирования");
            }

            // 5. Валидация дат
            LocalDateTime now = LocalDateTime.now();
            log.debug("Текущее время: {}", now);
            log.debug("Дата начала бронирования: {}", bookingDto.getStart());
            log.debug("Дата окончания бронирования: {}", bookingDto.getEnd());

            if (bookingDto.getStart().isAfter(bookingDto.getEnd())) {
                log.error("❌ Дата начала позже даты окончания: start={}, end={}",
                        bookingDto.getStart(), bookingDto.getEnd());
                throw new ValidationException("Дата начала должна быть раньше даты окончания");
            }

            if (bookingDto.getStart().isBefore(now)) {
                log.error("Дата начала в прошлом: start={}, now={}", bookingDto.getStart(), now);
                throw new ValidationException("Дата начала должна быть в будущем");
            }

            if (bookingDto.getEnd().isBefore(now)) {
                log.error("Дата окончания в прошлом: end={}, now={}", bookingDto.getEnd(), now);
                throw new ValidationException("Дата окончания должна быть в будущем");
            }

            //Проверяем, нет ли конфликтующих бронирований
            log.debug("Проверка конфликтующих бронирований для itemId={}", item.getId());
            List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                    item.getId(), bookingDto.getStart(), bookingDto.getEnd());

            if (!overlappingBookings.isEmpty()) {
                log.error("Найдены конфликтующие бронирования: count={}", overlappingBookings.size());
                overlappingBookings.forEach(b -> log.debug("Конфликтующее бронирование: id={}, start={}, end={}",
                        b.getId(), b.getStart(), b.getEnd()));
                throw new ValidationException("На выбранные даты уже есть бронирование");
            }

            //Создаем бронирование
            log.debug("Создание объекта Booking из DTO");
            Booking booking = bookingMapper.toBooking(bookingDto);
            booking.setItem(item);
            booking.setBooker(booker);
            booking.setStatus(BookingStatus.WAITING);
            booking.setCreatedDate(now);

            log.info("Сохранение бронирования в БД...");
            Booking savedBooking = bookingRepository.save(booking);

            log.info("Бронирование успешно создано: id={}, itemId={}, bookerId={}, status={}",
                    savedBooking.getId(), savedBooking.getItem().getId(),
                    savedBooking.getBooker().getId(), savedBooking.getStatus());

            BookingResponseDto response = bookingMapper.toBookingResponseDto(savedBooking);
            log.info("ОНЕЦ СОЗДАНИЯ БРОНИРОВАНИЯ");

            return response;

        } catch (NotFoundException | ValidationException | AccessDeniedException e) {
            // Логируем известные исключения
            log.error("Известное исключение при создании бронирования: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Логируем неизвестные исключения
            log.error("Неизвестная ошибка при создании бронирования", e);
            throw new ValidationException("Ошибка при создании бронирования: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BookingResponseDto bookingStatusUpdate(Long bookingId, Boolean approved, Long userId) {
        log.info("ОБНОВЛЕНИЕ СТАТУСА БРОНИРОВАНИЯ");
        log.info("bookingId: {}, approved: {}, userId: {}", bookingId, approved, userId);

        try {
            Booking booking = findById(bookingId);
            log.info("Найдено бронирование: id={}, status={}, itemOwnerId={}",
                    booking.getId(), booking.getStatus(), booking.getItem().getOwner().getId());

            // Проверяем, что пользователь - владелец вещи
            if (!booking.getItem().getOwner().getId().equals(userId)) {
                log.error("Пользователь не является владельцем: userId={}, ownerId={}",
                        userId, booking.getItem().getOwner().getId());
                throw new AccessDeniedException("Только владелец может подтверждать бронь");
            }

            // Проверяем текущий статус
            if (booking.getStatus() != BookingStatus.WAITING) {
                log.error("Статус бронирования уже изменен: currentStatus={}", booking.getStatus());
                throw new ValidationException("Статус брони уже изменен");
            }

            // Устанавливаем новый статус
            BookingStatus newStatus = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
            booking.setStatus(newStatus);

            log.info("Сохранение обновленного бронирования...");
            Booking updatedBooking = bookingRepository.save(booking);

            log.info("Статус бронирования обновлен: id={}, newStatus={}",
                    updatedBooking.getId(), updatedBooking.getStatus());

            return bookingMapper.toBookingResponseDto(updatedBooking);

        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса бронирования", e);
            throw e;
        }
    }

    @Override
    public BookingResponseDto getBookingById(Long bookingId, Long userId) {
        log.info("ПОЛУЧЕНИЕ БРОНИРОВАНИЯ ПО ID");
        log.info("bookingId: {}, userId: {}", bookingId, userId);

        try {
            Booking booking = findById(bookingId);
            log.info("Найдено бронирование: id={}, bookerId={}, itemOwnerId={}",
                    booking.getId(), booking.getBooker().getId(), booking.getItem().getOwner().getId());

            boolean isBooker = booking.getBooker().getId().equals(userId);
            boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

            log.debug("Проверка прав доступа: isBooker={}, isOwner={}", isBooker, isOwner);

            if (!isBooker && !isOwner) {
                log.error("Доступ запрещен: userId={} не является ни booker ни owner", userId);
                throw new AccessDeniedException("Доступ запрещен");
            }

            log.info("Бронирование доступно для пользователя: userId={}", userId);
            return bookingMapper.toBookingResponseDto(booking);

        } catch (Exception e) {
            log.error("Ошибка при получении бронирования по id", e);
            throw e;
        }
    }

    @Override
    public List<BookingResponseDto> getUserBookings(Long bookerId, BookingState state, int from, int size) {
        log.info("ПОЛУЧЕНИЕ БРОНИРОВАНИЙ ПОЛЬЗОВАТЕЛЯ");
        log.info("bookerId: {}, state: {}, from: {}, size: {}", bookerId, state, from, size);

        try {
            // Проверяем существование пользователя
            userRepository.findById(bookerId)
                    .orElseThrow(() -> {
                        log.error("Пользователь не найден: bookerId={}", bookerId);
                        return new NotFoundException("Пользователь не найден");
                    });

            Pageable pageable = PageRequest.of(from / size, size, Sort.by("start").descending());
            LocalDateTime now = LocalDateTime.now();
            List<Booking> bookings;

            log.debug("Фильтрация по состоянию: {}", state);
            switch (state) {
                case CURRENT:
                    log.debug("Поиск текущих бронирований");
                    bookings = bookingRepository.findByBookerIdAndStartBeforeAndEndAfter(bookerId, now, now, pageable);
                    break;
                case PAST:
                    log.debug("Поиск завершенных бронирований");
                    bookings = bookingRepository.findByBookerIdAndEndBefore(bookerId, now, pageable);
                    break;
                case FUTURE:
                    log.debug("Поиск будущих бронирований");
                    bookings = bookingRepository.findByBookerIdAndStartAfter(bookerId, now, pageable);
                    break;
                case WAITING:
                    log.debug("Поиск ожидающих бронирований");
                    bookings = bookingRepository.findByBookerIdAndStatus(bookerId, BookingStatus.WAITING, pageable);
                    break;
                case REJECTED:
                    log.debug("Поиск отклоненных бронирований");
                    bookings = bookingRepository.findByBookerIdAndStatus(bookerId, BookingStatus.REJECTED, pageable);
                    break;
                default: // "ALL"
                    log.debug("Поиск всех бронирований");
                    bookings = bookingRepository.findByBookerId(bookerId, pageable);
            }

            log.info("Найдено {} бронирований для пользователя {}", bookings.size(), bookerId);
            return bookings.stream()
                    .map(bookingMapper::toBookingResponseDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка при получении бронирований пользователя", e);
            throw e;
        }
    }

    @Override
    public List<BookingResponseDto> getOwnerBookings(Long ownerId, BookingState state, int from, int size) {
        log.info("ПОЛУЧЕНИЕ БРОНИРОВАНИЙ ВЛАДЕЛЬЦА");
        log.info("ownerId: {}, state: {}, from: {}, size: {}", ownerId, state, from, size);

        try {
            // Проверяем существование пользователя
            userRepository.findById(ownerId)
                    .orElseThrow(() -> {
                        log.error("Владелец не найден: ownerId={}", ownerId);
                        return new NotFoundException("Пользователь не найден");
                    });

            Pageable pageable = PageRequest.of(from / size, size, Sort.by("start").descending());
            LocalDateTime now = LocalDateTime.now();
            List<Booking> bookings;

            log.debug("Фильтрация по состоянию: {}", state);
            switch (state) {
                case CURRENT:
                    bookings = bookingRepository.findByItemOwnerIdAndStartBeforeAndEndAfter(ownerId, now, now, pageable);
                    break;
                case PAST:
                    bookings = bookingRepository.findByItemOwnerIdAndEndBefore(ownerId, now, pageable);
                    break;
                case FUTURE:
                    bookings = bookingRepository.findByItemOwnerIdAndStartAfter(ownerId, now, pageable);
                    break;
                case WAITING:
                    bookings = bookingRepository.findByItemOwnerIdAndStatus(ownerId, BookingStatus.WAITING, pageable);
                    break;
                case REJECTED:
                    bookings = bookingRepository.findByItemOwnerIdAndStatus(ownerId, BookingStatus.REJECTED, pageable);
                    break;
                default: // "ALL"
                    bookings = bookingRepository.findByItemOwnerId(ownerId, pageable);
            }

            log.info("Найдено {} бронирований для владельца {}", bookings.size(), ownerId);
            return bookings.stream()
                    .map(bookingMapper::toBookingResponseDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка при получении бронирований владельца", e);
            throw e;
        }
    }

    // Вспомогательный метод для поиска бронирования
    public Booking findById(Long bookingId) {
        log.debug("Поиск бронирования по id: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("Бронь не найдена: bookingId={}", bookingId);
                    return new NotFoundException("Бронь не найдена");
                });
        log.debug("Бронь найдена: id={}", booking.getId());
        return booking;
    }
}