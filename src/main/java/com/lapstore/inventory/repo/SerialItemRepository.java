package com.lapstore.inventory.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.inventory.domain.SerialItem;

public interface SerialItemRepository extends JpaRepository<SerialItem, Long> {

    /**
     * serial_number là UNIQUE toàn hệ thống. Nạp luôn variant và branch vì ngay sau
     * đó cần dùng.
     */
    @EntityGraph(attributePaths = { "variant", "branch" })
    Optional<SerialItem> findBySerialNumber(String serialNumber);
}
