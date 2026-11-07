package com.lapstore.inventory.dto;

/** linesJson: [{"variantId":1,"quantity":2,"unitCost":18500000,"serials":["SN1","SN2"]}]. confirm=false -> lưu nháp. */
public record StockReceiptRequest(Long supplierId, Long branchId, String note, String linesJson, boolean confirm) {}
