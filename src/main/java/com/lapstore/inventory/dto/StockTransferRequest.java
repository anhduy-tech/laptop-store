package com.lapstore.inventory.dto;

import java.util.List;

public record StockTransferRequest(Long fromBranchId, Long toBranchId, List<String> serials) {}
