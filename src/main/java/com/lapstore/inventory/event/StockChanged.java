package com.lapstore.inventory.event;

/**
 * Tồn của (variant, chi nhánh) vừa đổi. Phát trong transaction; chỉ nên đẩy realtime / gửi mail
 * bằng @TransactionalEventListener(phase = AFTER_COMMIT) để không báo về thay đổi đã bị rollback.
 */
public record StockChanged(Long variantId, Long branchId) {}
