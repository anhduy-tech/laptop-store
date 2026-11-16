package com.lapstore.inventory.service;

import java.io.InputStream;
import java.util.List;

import com.lapstore.inventory.dto.StockRow;
import com.lapstore.inventory.dto.StockSerialsView;
import com.lapstore.inventory.dto.StocktakeItemRow;
import com.lapstore.inventory.dto.StocktakeRow;

/**
 * Đọc/ghi file Excel (Apache POI) cho kho: nhập danh sách serial từ file, xuất tồn kho, serial và kết quả kiểm kê.
 * Cột serial của file xuất đặt ở cột A với tiêu đề "Serial" nên nhập lại được ngay.
 */
public interface InventoryExcelService {
    /** Số serial tối đa đọc từ một file. */
    int MAX_SERIALS = 5000;

    /**
     * Đọc danh sách serial từ sheet đầu tiên (.xlsx/.xls): cột có tiêu đề chứa "serial"/"imei", không có thì cột A; bỏ dòng
     * trống, bỏ trùng, giữ thứ tự. Ô số được đổi sang chuỗi không có phần thập phân.
     *
     * @throws com.lapstore.common.exception.BusinessException file hỏng, không phải Excel, hoặc quá nhiều dòng
     */
    List<String> readSerials(InputStream in);

    /** File mẫu: cột Serial định dạng văn bản (giữ nguyên số 0 đầu) và một sheet hướng dẫn. */
    byte[] serialTemplate();

    byte[] exportStock(List<StockRow> rows);

    /** @param withCost thêm cột giá vốn (chỉ truyền true cho người có quyền COST_VIEW) */
    byte[] exportSerials(StockSerialsView view, boolean withCost);

    byte[] exportStocktake(StocktakeRow header, List<StocktakeItemRow> items);
}
