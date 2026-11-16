package com.lapstore.inventory.dto;

/**
 * Một phiên kiểm kê. status: OPEN | CLOSED | CANCELLED. expected = số máy hệ thống ghi đang còn hàng tại chi nhánh lúc bắt đầu;
 * scanned = số máy trong danh sách đó đã quét thấy; missing chỉ có sau khi đóng phiên; unexpected = máy quét thấy nhưng
 * không nằm trong danh sách chi nhánh (hệ thống ghi ở nơi khác hoặc trạng thái khác).
 */
public record StocktakeRow(String code, String branchName, String status, String scope, String startedAt,
        String startedBy, String closedAt, String closedBy, long expected, long scanned, long missing, long unexpected,
        long adjusted) {

    public boolean open() {
        return "OPEN".equals(status);
    }

    public boolean closed() {
        return "CLOSED".equals(status);
    }

    /** Chưa quét thấy (khi đang mở) — sau khi đóng chính là số máy thiếu. */
    public long remaining() {
        return expected - scanned;
    }

    public String statusLabel() {
        return switch (status) {
            case "OPEN" -> "Đang kiểm";
            case "CLOSED" -> "Đã đóng";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}
