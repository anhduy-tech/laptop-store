package com.lapstore.inventory.dto;

import java.util.List;

/** Kết quả một lần quét: recorded = số máy ghi nhận mới, already = đã quét trước đó, unexpected = trong số mới có bao nhiêu máy dư, unknown = serial không có trong hệ thống. */
public record ScanResult(int recorded, int already, int unexpected, List<String> unknown) {
}
