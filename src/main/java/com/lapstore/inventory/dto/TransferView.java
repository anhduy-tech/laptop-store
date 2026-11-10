package com.lapstore.inventory.dto;

import java.util.List;

/** Chi tiết một phiếu chuyển kho kèm danh sách máy. */
public record TransferView(TransferRow header, String note, List<TransferItemRow> items) {
}
