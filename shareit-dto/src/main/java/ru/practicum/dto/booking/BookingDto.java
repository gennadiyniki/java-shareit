package ru.practicum.dto.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingDto {

    @NotNull(message = "ID вещи обязателен")
    private Long itemId;

    @NotNull(message = "Дата начала бронирования должна быть указана")
    @FutureOrPresent(message = "Дата начала должна быть в настоящем или будущем")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime start;

    @NotNull(message = "Дата окончания бронирования должна быть указана")
    @Future(message = "Дата окончания должна быть в будущем")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime end;

    @AssertTrue(message = "Дата окончания должна быть после даты начала")
    public boolean isEndAfterStart() {
        return end != null && start != null && end.isAfter(start);
    }
}