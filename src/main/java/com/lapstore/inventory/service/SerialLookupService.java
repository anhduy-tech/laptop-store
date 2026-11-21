package com.lapstore.inventory.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lapstore.inventory.dto.SerialDetailView;
import com.lapstore.inventory.dto.SerialSearchRow;

/** Tra cứu serial (WH-09). Quyền PORTAL_WAREHOUSE. */
public interface SerialLookupService {
    /** Số ký tự tối thiểu của từ khóa tìm serial (ngắn hơn trả kết quả rỗng để khỏi quét cả bảng). */
    int MIN_QUERY = 2;

    /**
     * Tìm serial chứa từ khóa (không phân biệt hoa thường); khớp tuyệt đối xếp lên đầu.
     *
     * @param status IN_STOCK | IN_TRANSIT | SOLD | WARRANTY | RETURNED | DEFECTIVE; null = tất cả
     */
    Page<SerialSearchRow> search(String q, String status, Pageable pageable);

    /** Serial trùng khớp hoàn toàn (không phân biệt hoa thường) với từ khóa, nếu có: dùng cho quét mã vạch ở ô tìm nhanh. */
    Optional<String> findExact(String q);

    /** @throws com.lapstore.common.exception.NotFoundException nếu không có serial này */
    SerialDetailView detail(String serialNumber);
}
