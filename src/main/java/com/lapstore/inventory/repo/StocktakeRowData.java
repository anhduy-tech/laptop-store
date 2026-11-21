package com.lapstore.inventory.repo;

import java.time.Instant;

/** Kết quả truy vấn thô danh sách phiên kiểm kê; các số đếm để Number vì COUNT trả Long. */
public record StocktakeRowData(String code, String branchName, String status, String scope, Instant startedAt,
        String startedBy, Instant closedAt, String closedBy, Number expected, Number scanned, Number missing,
        Number unexpected, Number adjusted) {
}
