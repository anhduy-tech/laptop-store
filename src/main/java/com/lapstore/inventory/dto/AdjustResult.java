package com.lapstore.inventory.dto;

import java.util.List;

/** Kết quả áp dụng điều chỉnh sau kiểm kê. skipped là các dòng "serial: lý do" không điều chỉnh được. */
public record AdjustResult(int relocated, int writtenOff, List<String> skipped) {
}
