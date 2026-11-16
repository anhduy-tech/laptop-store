package com.lapstore.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hồ sơ một máy theo serial: ở đâu, trạng thái gì, nhập từ đâu, bán cho đơn nào, đang bị giữ không, và toàn bộ lịch sử.
 * costPrice chỉ có giá trị với người có quyền COST_VIEW (service tự bỏ nếu không). Các trường thời gian đã định dạng, rỗng nếu chưa có.
 */
public record SerialDetailView(String serialNumber, String status, Long productId, String productName, String brandName,
        Long variantId, String sku, String variantName, String colorName, String cpu, String gpu, Integer ramGb,
        Integer storageGb, String branchName, BigDecimal costPrice, String receiptCode, String supplierName,
        String receivedAt, String orderCode, String soldAt, String warrantyEnd, HoldInfo hold,
        List<HistoryRow> history, List<StocktakeRef> stocktakes, List<TransferRef> transfers) {

    /** Hold đang ACTIVE (giữ cho đơn hoặc đang ở quầy). */
    public record HoldInfo(String holdType, String orderCode, String heldBy, String heldAt, String expiresAt) {
    }

    /** refType: RECEIPT | TRANSFER | STOCKTAKE | ... ; refCode là mã phiếu tương ứng nếu tra được. */
    public record HistoryRow(String at, String fromStatus, String toStatus, String fromBranch, String toBranch,
            String refType, String refCode, String by) {
    }

    public record StocktakeRef(String code, String branchName, String result, String scannedAt) {
    }

    public record TransferRef(String code, String status, String fromBranch, String toBranch) {
    }
}
