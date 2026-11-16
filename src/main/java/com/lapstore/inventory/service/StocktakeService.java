package com.lapstore.inventory.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lapstore.inventory.dto.AdjustResult;
import com.lapstore.inventory.dto.ScanResult;
import com.lapstore.inventory.dto.StocktakeItemRow;
import com.lapstore.inventory.dto.StocktakeRow;
import com.lapstore.inventory.dto.StocktakeView;

/**
 * Kiểm kê kho theo chi nhánh. Luồng: bắt đầu (chụp danh sách máy hệ thống ghi đang còn hàng tại chi nhánh) → quét serial
 * thực tế (nhiều lần, nhập tay hoặc từ Excel) → đóng phiên (máy chưa quét thành "Thiếu") → áp dụng điều chỉnh.
 * Xem: PORTAL_WAREHOUSE. Bắt đầu/quét/đóng/hủy/điều chỉnh: STOCK_STOCKTAKE.
 */
public interface StocktakeService {
    /** @param status OPEN | CLOSED | CANCELLED; null = tất cả */
    Page<StocktakeRow> list(String status, Pageable pageable);

    /** @param filter null | MATCHED | PENDING | UNEXPECTED */
    StocktakeView get(String code, String filter, Pageable pageable);

    /** Toàn bộ dòng của phiên (xuất Excel). */
    List<StocktakeItemRow> allItems(String code);

    /** @return mã phiên. Mỗi chi nhánh chỉ có một phiên đang mở. */
    String start(Long branchId, String scope, String username);

    ScanResult scan(String code, List<String> serials, String username);

    /** Máy chưa quét của danh sách ban đầu thành "Thiếu"; phiên chuyển sang CLOSED. */
    void close(String code, String username);

    void cancel(String code, String username);

    /**
     * Sau khi đóng: máy THIẾU còn IN_STOCK tại chi nhánh -> trạng thái DEFECTIVE và trừ tồn (schema chưa có trạng thái "mất");
     * máy DƯ đang IN_STOCK ở chi nhánh khác -> chuyển về chi nhánh này. Dòng không điều chỉnh được nằm trong skipped.
     */
    AdjustResult adjust(String code, String username);
}
