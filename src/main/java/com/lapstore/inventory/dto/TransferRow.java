package com.lapstore.inventory.dto;

/** Một dòng danh sách phiếu chuyển kho. status: IN_TRANSIT | RECEIVED. Thời gian đã định dạng, rỗng nếu chưa có. */
public record TransferRow(String code, String fromBranch, String toBranch, String status, long itemCount,
        String shippedAt, String shippedBy, String receivedAt, String receivedBy) {

    public boolean inTransit() {
        return "IN_TRANSIT".equals(status);
    }
}
