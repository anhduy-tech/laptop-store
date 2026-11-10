package com.lapstore.inventory.repo;

import java.time.Instant;

/** Kết quả truy vấn thô danh sách phiếu chuyển; số máy để Number vì COUNT trả Long. */
public record TransferRowData(String code, String fromBranch, String toBranch, String status, Number itemCount,
        Instant shippedAt, String shippedBy, Instant receivedAt, String receivedBy) {
}
