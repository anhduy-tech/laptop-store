package com.lapstore.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

import com.lapstore.catalog.dto.NewProductSpec;

/**
 * Phiếu nhập: nhiều sản phẩm, mỗi sản phẩm nhiều biến thể (tổ hợp màu x CPU x RAM x ổ cứng),
 * mỗi biến thể có giá vốn, (giá bán nếu là biến thể mới) và danh sách serial. Biến thể không có serial bị bỏ qua.
 */
public record StockReceiptRequest(Long supplierId, Long branchId, String note, List<ProductLine> products) {

    /** productId có giá trị = sản phẩm đã có; ngược lại dùng newProduct để tạo mới. gpu áp dụng cho các biến thể mới. */
    public record ProductLine(Long productId, NewProductSpec newProduct, String gpu, List<VariantLine> variants) {
    }

    public record VariantLine(Long colorId, String cpu, Integer ramGb, Integer storageGb,
            BigDecimal unitCost, BigDecimal sellPrice, List<String> serials) {
    }
}
