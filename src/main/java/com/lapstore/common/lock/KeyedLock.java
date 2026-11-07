package com.lapstore.common.lock;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.lapstore.common.exception.LockBusyException;

/**
 * Khóa trong bộ nhớ theo KHÓA NGHIỆP VỤ (vd "serial:123"): hai máy khác nhau
 * không chặn nhau.
 *
 * QUY TẮC VÀNG (mục 4.3): khóa phải nằm NGOÀI transaction.
 * lấy khóa -> mở transaction -> ghi -> COMMIT -> nhả khóa.
 * Vì vậy bean gọi withLock KHÔNG được có @Transactional; phần ghi DB nằm ở một
 * bean khác (có @Transactional)
 * và được gọi bên trong lambda - transaction commit xong rồi lambda mới trả về,
 * lúc đó khóa mới nhả.
 *
 * Chỉ có tác dụng trong MỘT tiến trình JVM. Chạy nhiều bản ứng dụng cùng một DB
 * thì vẫn cần ràng buộc UNIQUE ở DB
 * làm lưới an toàn (xem mục 4.4).
 */
@Component
public class KeyedLock {

    private static final class Entry {
        final ReentrantLock lock = new ReentrantLock();
        int refs; // số luồng đang giữ hoặc đang chờ; chỉ đổi trong locks.compute nên an toàn
    }

    private final ConcurrentHashMap<Object, Entry> locks = new ConcurrentHashMap<>();

    /** Chạy action khi giữ khóa của key. Quá thời gian chờ -> LockBusyException. */
    public <T> T withLock(Object key, Duration wait, Supplier<T> action) {
        Entry e = locks.compute(key, (k, cur) -> {
            Entry x = (cur != null) ? cur : new Entry();
            x.refs++;
            return x;
        });
        boolean locked = false;
        try {
            locked = e.lock.tryLock(wait.toMillis(), TimeUnit.MILLISECONDS);
            if (!locked)
                throw new LockBusyException(key);
            return action.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LockBusyException(key);
        } finally {
            if (locked)
                e.lock.unlock();
            // dọn entry khi không còn ai dùng, tránh map phình ra theo số serial
            locks.computeIfPresent(key, (k, cur) -> (--cur.refs == 0) ? null : cur);
        }
    }

    public void withLock(Object key, Duration wait, Runnable action) {
        withLock(key, wait, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Lấy NHIỀU khóa (vd chốt đơn có nhiều serial). Luôn lấy theo thứ tự cố định để
     * hai luồng
     * không giữ chéo nhau gây deadlock. Thời gian chờ áp cho từng khóa.
     */
    public <T> T withLocks(Collection<?> keys, Duration wait, Supplier<T> action) {
        List<Object> ordered = keys.stream()
                .<Object>map(k -> k)
                .distinct()
                .sorted(Comparator.comparing(Object::toString))
                .toList();
        return nest(ordered, 0, wait, action);
    }

    private <T> T nest(List<Object> keys, int i, Duration wait, Supplier<T> action) {
        if (i == keys.size())
            return action.get();
        return withLock(keys.get(i), wait, () -> nest(keys, i + 1, wait, action));
    }

    /** Số khóa đang tồn tại (để kiểm tra rò rỉ; về 0 khi không ai dùng). */
    public int activeKeys() {
        return locks.size();
    }
}
