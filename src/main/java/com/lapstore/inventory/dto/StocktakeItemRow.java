package com.lapstore.inventory.dto;

/** Một máy trong phiên kiểm kê. result: MATCHED | MISSING | UNEXPECTED | null (chưa quét). */
public record StocktakeItemRow(String serialNumber, String productName, String sku, String variantName,
        boolean expected, String result, String systemStatus, String systemBranch, String scannedAt, String note,
        boolean adjusted) {

    public String resultLabel() {
        if (result == null)
            return expected ? "Chưa quét" : "";
        return switch (result) {
            case "MATCHED" -> "Khớp";
            case "MISSING" -> "Thiếu";
            case "UNEXPECTED" -> "Dư";
            default -> result;
        };
    }
}
