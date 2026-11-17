package com.lapstore.inventory.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lapstore.common.exception.LockBusyException;
import com.lapstore.common.lock.KeyedLock;
import com.lapstore.inventory.repo.StockHoldRepository;

/**
 * Job nền dọn hold quá hạn (BR-04, BR-11): đánh dấu EXPIRED và trả held_qty.
 * Cố ý KHÔNG có @Transactional: với hold SERIAL phải lấy khóa "serial:SN" (chung khóa với giữ máy, chuyển kho,
 * kiểm kê) rồi mới gọi HoldTx.expire (transaction riêng) — cùng mẫu HoldService/HoldTx.
 * Hold lỗi lẻ không làm hỏng cả lượt: bỏ qua, lượt sau thử lại.
 */
@Component
class HoldExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(HoldExpiryJob.class);
    private static final Duration WAIT = Duration.ofSeconds(3);

    private final StockHoldRepository holds;
    private final HoldTx tx;
    private final KeyedLock locks;

    HoldExpiryJob(StockHoldRepository holds, HoldTx tx, KeyedLock locks) {
        this.holds = holds;
        this.tx = tx;
        this.locks = locks;
    }

    @Scheduled(fixedDelayString = "${app.hold.expiry-interval-ms:30000}")
    public void expireDue() {
        runOnce();
    }

    /** Tách ra để test gọi trực tiếp. @return số hold đã dọn. */
    int runOnce() {
        List<Long> ids = holds.findExpiredIds(Instant.now());
        int done = 0;
        for (Long id : ids) {
            try {
                if (expireOne(id)) done++;
            } catch (LockBusyException e) {
                log.debug("Hold {} đang bận, thử lại lượt sau", id);
            } catch (RuntimeException e) {
                log.warn("Không dọn được hold {}: {}", id, e.toString());
            }
        }
        if (done > 0) log.info("Đã dọn {} hold hết hạn", done);
        return done;
    }

    private boolean expireOne(Long id) {
        // Hold QUANTITY (đơn online) không có serial -> không cần khóa theo serial.
        String sn = holds.findSerialNumberByHoldId(id).orElse(null);
        if (sn == null) return tx.expire(id);
        return locks.withLock("serial:" + sn, WAIT, () -> tx.expire(id));
    }
}
