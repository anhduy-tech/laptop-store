package com.lapstore.inventory.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.inventory.domain.SerialItem;

public interface SerialItemRepository extends JpaRepository<SerialItem, Long> {

    /**
     * serial_number là UNIQUE toàn hệ thống. Nạp luôn variant và branch vì ngay sau
     * đó cần dùng.
     */
    @EntityGraph(attributePaths = { "variant", "branch" })
    Optional<SerialItem> findBySerialNumber(String serialNumber);

    /** Các serial (trong danh sách) đã tồn tại — dùng kiểm tra trùng khi nhập kho (BR-01). Tối đa 500 phần tử. */
    @Query("select s.serialNumber from SerialItem s where s.serialNumber in :sns")
    List<String> findExistingSerialNumbers(@Param("sns") Collection<String> sns);

    @EntityGraph(attributePaths = { "variant", "branch" })
    List<SerialItem> findBySerialNumberIn(Collection<String> serialNumbers);
}
