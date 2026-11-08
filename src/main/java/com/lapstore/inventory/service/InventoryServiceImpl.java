package com.lapstore.inventory.service;

import java.time.Duration;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.lapstore.common.dto.IdName;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.lock.KeyedLock;
import com.lapstore.inventory.dto.ReceiptResult;
import com.lapstore.inventory.dto.StockReceiptRequest;
import com.lapstore.inventory.dto.StockTransferRequest;

/**
 * Lớp NGOÀI: kiểm quyền, chuẩn hóa đầu vào, lấy KeyedLock "serial:..." rồi gọi InventoryTx.
 * Cố ý KHÔNG có @Transactional (xem KeyedLock): commit xong mới nhả khóa, nhờ đó chuyển kho
 * không chen được vào giữa lúc quầy đang giữ cùng một máy (HoldService dùng chung khóa này).
 * Nhập kho không cần khóa: serial mới chưa ai giữ, UNIQUE(serial_number) chặn trùng.
 */
@Service
class InventoryServiceImpl implements InventoryService {
    private static final Duration WAIT = Duration.ofSeconds(5);

    private final KeyedLock locks;
    private final InventoryTx tx;

    InventoryServiceImpl(KeyedLock locks, InventoryTx tx) {
        this.locks = locks;
        this.tx = tx;
    }

    @Override
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public List<IdName> suppliers() {
        return tx.suppliers();
    }

    @Override
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public List<IdName> branches() {
        return tx.branches();
    }

    @Override
    @PreAuthorize("hasAuthority('STOCK_RECEIVE')")
    public ReceiptResult createReceipt(StockReceiptRequest request, String username) {
        return tx.createReceipt(request, username);
    }

    @Override
    @PreAuthorize("hasAuthority('STOCK_TRANSFER')")
    public String transfer(StockTransferRequest request, String username) {
        List<String> sns = normalize(request.serials());
        StockTransferRequest clean = new StockTransferRequest(request.fromBranchId(), request.toBranchId(), sns);
        return locks.withLocks(keys(sns), WAIT, () -> tx.transfer(clean, username));
    }

    @Override
    @PreAuthorize("hasAuthority('STOCK_TRANSFER')")
    public void receiveTransfer(String transferCode, String username) {
        if (transferCode == null || transferCode.isBlank())
            throw new BusinessException("Thiếu mã phiếu chuyển.");
        String code = transferCode.trim();
        List<String> sns = tx.transferSerials(code);
        locks.withLocks(keys(sns), WAIT, () -> {
            tx.receiveTransfer(code, username);
            return null;
        });
    }

    private static List<String> normalize(List<String> serials) {
        List<String> sns = serials == null ? List.of()
                : serials.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList();
        if (sns.isEmpty())
            throw new BusinessException("Nhập ít nhất một serial.");
        if (sns.size() > InventoryTx.MAX_SERIALS)
            throw new BusinessException("Mỗi phiếu chuyển tối đa " + InventoryTx.MAX_SERIALS + " serial.");
        return sns;
    }

    private static List<String> keys(List<String> serials) {
        return serials.stream().map(s -> "serial:" + s).toList();
    }
}
