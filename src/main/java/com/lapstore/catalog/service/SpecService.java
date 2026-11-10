package com.lapstore.catalog.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.lapstore.catalog.dto.SpecChoices;
import com.lapstore.catalog.dto.SpecKind;
import com.lapstore.catalog.dto.SpecOptionRow;
import com.lapstore.catalog.dto.SpecUsageView;

/**
 * Quản lý giá trị CPU / GPU / RAM / ổ cứng trên bảng attribute_definition + attribute_option có sẵn (không thêm bảng).
 * Xem: PORTAL_WAREHOUSE. Thêm/sửa tên/xóa: PRODUCT_MANAGE. Chống trùng theo khóa chuẩn hóa: không phân biệt hoa thường,
 * dấu, khoảng trắng, dấu gạch ("Core i5-1335U" = "core i5 1335u"); RAM/ổ cứng so theo số GB ("1TB" = "1024").
 */
public interface SpecService {
    /** Tạo nhóm/định nghĩa thuộc tính nếu chưa có và nạp giá trị từ các biến thể hiện có. Gọi lúc khởi động. */
    void initialize();

    List<SpecOptionRow> list(SpecKind kind);

    /** Giá trị đang dùng được, để đổ vào ô chọn. */
    SpecChoices choices();

    /** Các biến thể/sản phẩm đang dùng một giá trị (phân trang). @throws NotFoundException nếu không có giá trị */
    SpecUsageView usage(long optionId, Pageable pageable);

    /** Trùng (khóa chuẩn hóa) -> BusinessException nêu giá trị đã có. */
    void add(SpecKind kind, String raw);

    /** Chỉ CPU/GPU. Cập nhật luôn các biến thể đang dùng. Trùng giá trị khác -> BusinessException. */
    void rename(long optionId, String newValue);

    /** Chỉ khi không còn biến thể nào dùng giá trị này. */
    void delete(long optionId);

    /**
     * Dùng nội bộ khi nhập kho/sửa sản phẩm (không kiểm quyền ở đây): tìm giá trị theo khóa chuẩn hóa, có rồi thì dùng
     * luôn giá trị đã có; chưa có và createIfMissing thì tạo (cần PRODUCT_MANAGE), không thì BusinessException.
     */
    SpecChoices.Choice resolve(SpecKind kind, String raw, boolean createIfMissing);
}
