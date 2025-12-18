package ru.practicum.server.booking;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.booking.BookingDto;
import ru.practicum.dto.booking.BookingResponseDto;
import ru.practicum.dto.item.ItemDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.server.item.Item;
import ru.practicum.server.user.User;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "item.id", target = "itemId")
    BookingDto toBookingDto(Booking booking);

    @Mapping(target = "item", ignore = true)
    @Mapping(target = "booker", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "id", ignore = true)
    Booking toBooking(BookingDto bookingDto);

    @Mapping(source = "createdDate", target = "created")
    @Mapping(source = "item", target = "item")
    @Mapping(source = "booker", target = "booker")
    BookingResponseDto toBookingResponseDto(Booking booking);

    // для преобразования Item в ItemDto
    ItemDto toItemDto(Item item);

    // для преобразования User в UserDto
    UserDto toUserDto(User user);
}
