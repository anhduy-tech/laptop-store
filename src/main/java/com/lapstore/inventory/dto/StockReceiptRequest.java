package com.lapstore.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

import com.lapstore.catalog.dto.NewProductSpec;

/**
 * Phiếu nhập: nhiều sản phẩm, mỗi sản phẩm nhiều biến thể (tổ hợp màu x CPU x GPU x RAM x ổ cứng),
 * mỗi biến thể có giá vốn, (giá bán nếu là biến thể mới) và danh sách serial. Biến thể không có serial bị bỏ qua.
 */
public record StockReceiptRequest(Long supplierId, Long branchId, String note, List<ProductLine> products) {

    /** productId có giá trị = sản phẩm đã có; ngược lại dùng newProduct để tạo mới. */
    public record ProductLine(Long productId, NewProductSpec newProduct, List<VariantLine> variants) {
    }

    /** gpu trống = không có GPU rời. cpu/gpu/ramGb/storageGb phải là giá trị trong danh mục thuộc tính. */
    public record VariantLine(Long colorId, String cpu, String gpu, Integer ramGb, Integer storageGb,
            BigDecimal unitCost, BigDecimal sellPrice, List<String> serials) {
    }
}
