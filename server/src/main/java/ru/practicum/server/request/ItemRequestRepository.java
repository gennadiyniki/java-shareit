package ru.practicum.server.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    // Найти все запросы пользователя, отсортированные по дате
    List<ItemRequest> findByRequesterIdOrderByCreatedDesc(Long requesterId);

    @Query("SELECT DISTINCT ir FROM ItemRequest ir " +
            "LEFT JOIN FETCH ir.items " +
            "WHERE ir.requester.id = :requesterId " +
            "ORDER BY ir.created DESC")
    List<ItemRequest> findByRequesterIdWithItems(@Param("requesterId") Long requesterId);

    // Поиск всех запросов, созданных не указанным пользователем
    Page<ItemRequest> findByRequesterIdNot(Long requesterId, Pageable pageable);

    @Query("SELECT DISTINCT ir FROM ItemRequest ir " +
            "LEFT JOIN FETCH ir.items " +
            "WHERE ir.requester.id <> :requesterId")
    Page<ItemRequest> findByRequesterIdNotWithItems(@Param("requesterId") Long requesterId, Pageable pageable);

    @Query("SELECT DISTINCT ir FROM ItemRequest ir " +
            "LEFT JOIN FETCH ir.items i " +
            "LEFT JOIN FETCH i.owner " +
            "WHERE ir.id = :requestId")
    Optional<ItemRequest> findByIdWithItems(@Param("requestId") Long requestId);
}