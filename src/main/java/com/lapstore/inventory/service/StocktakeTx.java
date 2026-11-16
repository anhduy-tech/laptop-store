package com.lapstore.inventory.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.domain.InventoryBalance;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.SerialStatusHistory;
import com.lapstore.inventory.domain.Stocktake;
import com.lapstore.inventory.domain.StocktakeItem;
import com.lapstore.inventory.dto.AdjustResult;
import com.lapstore.inventory.dto.ScanResult;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.SerialItemRepository;
import com.lapstore.inventory.repo.SerialStatusHistoryRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.inventory.repo.StocktakeItemRepository;
import com.lapstore.inventory.repo.StocktakeRepository;
import com.lapstore.staff.domain.AppUser;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Lớp TRONG của kiểm kê: mỗi thao tác ghi DB nằm trong một transaction. Điều chỉnh (adjust) phải được gọi từ
 * StocktakeServiceImpl khi đã giữ KeyedLock "serial:..." (cùng khóa với giữ máy và chuyển kho) — đừng gọi trực tiếp.
 */
@Service
public class StocktakeTx {
    static final int MAX_SCAN = 2000;
    private static final int CHUNK = 500;
    private static final String IN_STOCK = "IN_STOCK";
    private static final String OPEN = "OPEN";
    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final StocktakeRepository stocktakes;
    private final StocktakeItemRepository items;
    private final SerialItemRepository serials;
    private final SerialStatusHistoryRepository histories;
    private final InventoryBalanceRepository balances;
    private final StockHoldRepository holds;
    private final ApplicationEventPublisher events;

    @PersistenceContext
    private EntityManager em;

    StocktakeTx(StocktakeRepository stocktakes, StocktakeItemRepository items, SerialItemRepository serials,
            SerialStatusHistoryRepository histories, InventoryBalanceRepository balances, StockHoldRepository holds,
            ApplicationEventPublisher events) {
        this.stocktakes = stocktakes;
        this.items = items;
        this.serials = serials;
        this.histories = histories;
        this.balances = balances;
        this.holds = holds;
        this.events = events;
    }

    // ---------------------------------------------------------------- bắt đầu

    @Transactional
    public String start(Long branchId, String scope, String username) {
        Instant now = Instant.now();
        AppUser user = user(username);
        Branch branch = branchId == null ? null : em.find(Branch.class, branchId);
        if (branch == null || !branch.isActive())
            throw new BusinessException("Chi nhánh kiểm kê không hợp lệ hoặc đã ngừng hoạt động.");
        if (stocktakes.existsByBranch_IdAndStatus(branchId, OPEN))
            throw new BusinessException("Chi nhánh " + branch.getName()
                    + " đang có một phiên kiểm kê chưa đóng. Đóng hoặc hủy phiên đó trước khi tạo phiên mới.");

        List<SerialItem> inStock = em.createQuery("select s from SerialItem s join fetch s.variant "
                + "where s.branch.id = :b and s.status = 'IN_STOCK' order by s.id", SerialItem.class)
                .setParameter("b", branchId).getResultList();
        if (inStock.isEmpty())
            throw new BusinessException("Chi nhánh " + branch.getName() + " không có máy nào đang còn hàng để kiểm.");

        Stocktake st = new Stocktake();
        st.setCode(code("KK"));
        st.setBranch(branch);
        st.setStatus(OPEN);
        String sc = scope == null ? "" : scope.trim();
        st.setScope(sc.isEmpty() ? "Toàn bộ chi nhánh" : (sc.length() > 100 ? sc.substring(0, 100) : sc));
        st.setStartedBy(user);
        st.setStartedAt(now);
        stocktakes.save(st);

        // chụp danh sách "hệ thống ghi đang có" tại thời điểm bắt đầu
        List<StocktakeItem> batch = new ArrayList<>();
        for (SerialItem s : inStock) {
            StocktakeItem it = new StocktakeItem();
            it.setStocktake(st);
            it.setSerial(s);
            it.setVariant(s.getVariant());
            it.setExpected(true);
            batch.add(it);
            if (batch.size() >= CHUNK) {
                items.saveAll(batch);
                items.flush();
                batch.clear();
            }
        }
        if (!batch.isEmpty())
            items.saveAll(batch);
        return st.getCode();
    }

    // ---------------------------------------------------------------- quét

    @Transactional
    public ScanResult scan(String code, List<String> serialNumbers, String username) {
        Stocktake st = require(code);
        if (!OPEN.equals(st.getStatus()))
            throw new BusinessException("Phiên " + code + " đã đóng, không quét thêm được.");
        Set<String> sns = new LinkedHashSet<>();
        for (String s : serialNumbers)
            if (s != null && !s.isBlank())
                sns.add(s.trim());
        if (sns.isEmpty())
            throw new BusinessException("Nhập ít nhất một serial.");
        if (sns.size() > MAX_SCAN)
            throw new BusinessException("Mỗi lần quét tối đa " + MAX_SCAN + " serial.");

        Instant now = Instant.now();
        AppUser user = user(username);
        List<String> all = new ArrayList<>(sns);
        Map<String, SerialItem> found = new HashMap<>();
        for (int i = 0; i < all.size(); i += CHUNK)
            for (SerialItem s : serials.findBySerialNumberIn(all.subList(i, Math.min(i + CHUNK, all.size()))))
                found.put(s.getSerialNumber(), s);
        List<String> unknown = all.stream().filter(s -> !found.containsKey(s)).toList();

        List<Long> ids = found.values().stream().map(SerialItem::getId).collect(Collectors.toList());
        Map<Long, StocktakeItem> existing = new HashMap<>();
        for (int i = 0; i < ids.size(); i += CHUNK)
            for (StocktakeItem it : items.findByStocktake_IdAndSerial_IdIn(st.getId(),
                    ids.subList(i, Math.min(i + CHUNK, ids.size()))))
                existing.put(it.getSerial().getId(), it);

        int recorded = 0;
        int already = 0;
        int unexpected = 0;
        for (SerialItem s : found.values()) {
            StocktakeItem it = existing.get(s.getId());
            if (it != null) {
                if (it.getScannedAt() != null) {
                    already++;
                    continue;
                }
                it.setScannedAt(now);
                it.setScannedBy(user);
                it.setResult(it.isExpected() ? "MATCHED" : "UNEXPECTED");
                recorded++;
            } else {
                StocktakeItem n = new StocktakeItem();
                n.setStocktake(st);
                n.setSerial(s);
                n.setVariant(s.getVariant());
                n.setExpected(false);
                n.setScannedAt(now);
                n.setScannedBy(user);
                n.setResult("UNEXPECTED");
                n.setNote("Hệ thống ghi: " + s.getStatus() + " tại " + s.getBranch().getName());
                items.save(n);
                recorded++;
                unexpected++;
            }
        }
        try {
            items.flush(); // UNIQUE(stocktake_id, serial_id) chặn hai người cùng thêm một dòng dư
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Có người vừa quét trùng những serial này. Thử lại.");
        }
        return new ScanResult(recorded, already, unexpected, unknown);
    }

    // ---------------------------------------------------------------- đóng / hủy

    @Transactional
    public void close(String code, String username) {
        Stocktake st = require(code);
        if (!OPEN.equals(st.getStatus()))
            throw new BusinessException("Phiên " + code + " không ở trạng thái đang kiểm.");
        AppUser user = user(username);
        em.createQuery("update StocktakeItem i set i.result = 'MISSING' where i.stocktake.id = :id "
                + "and i.expected = true and i.scannedAt is null").setParameter("id", st.getId()).executeUpdate();
        st.setStatus("CLOSED");
        st.setClosedBy(user);
        st.setClosedAt(Instant.now());
    }

    @Transactional
    public void cancel(String code, String username) {
        Stocktake st = require(code);
        if (!OPEN.equals(st.getStatus()))
            throw new BusinessException("Chỉ hủy được phiên đang kiểm.");
        st.setStatus("CANCELLED");
        st.setClosedBy(user(username));
        st.setClosedAt(Instant.now());
    }

    // ---------------------------------------------------------------- điều chỉnh

    /** Serial của các dòng thiếu/dư chưa điều chỉnh, để service ngoài lấy khóa trước. */
    @Transactional(readOnly = true)
    public List<String> adjustableSerials(String code) {
        Stocktake st = require(code);
        return items.findAdjustable(st.getId()).stream().map(i -> i.getSerial().getSerialNumber()).toList();
    }

    @Transactional
    public AdjustResult adjust(String code, String username) {
        Stocktake st = require(code);
        if (!"CLOSED".equals(st.getStatus()))
            throw new BusinessException("Chỉ áp dụng điều chỉnh sau khi đóng phiên kiểm kê.");
        Instant now = Instant.now();
        AppUser user = user(username);
        Branch branch = st.getBranch();
        int relocated = 0;
        int writtenOff = 0;
        List<String> skipped = new ArrayList<>();

        for (StocktakeItem it : items.findAdjustable(st.getId())) {
            SerialItem s = it.getSerial();
            ProductVariant v = s.getVariant();
            String sn = s.getSerialNumber();
            if (holds.existsBySerial_IdAndStatus(s.getId(), "ACTIVE")) {
                skipped.add(sn + ": đang được giữ cho đơn");
                continue;
            }
            if ("MISSING".equals(it.getResult())) {
                if (!IN_STOCK.equals(s.getStatus()) || !s.getBranch().getId().equals(branch.getId())) {
                    skipped.add(sn + ": đã đổi trạng thái hoặc chi nhánh sau lúc kiểm kê (" + s.getStatus() + ")");
                    continue;
                }
                if (!tryAddOnHand(v, branch, -1, now)) {
                    skipped.add(sn + ": tồn khả dụng không đủ để trừ (đang giữ cho đơn)");
                    continue;
                }
                s.setStatus("DEFECTIVE");
                histories.save(history(s, IN_STOCK, "DEFECTIVE", branch, null, st.getId(), user, now));
                it.setNote(append(it.getNote(), "Đã điều chỉnh: chuyển DEFECTIVE vì không tìm thấy khi kiểm kê"));
                it.setAdjustedBy(user);
                events.publishEvent(new StockChanged(v.getId(), branch.getId()));
                writtenOff++;
            } else { // UNEXPECTED
                if (!IN_STOCK.equals(s.getStatus())) {
                    skipped.add(sn + ": trạng thái " + s.getStatus() + " cần xử lý thủ công");
                    continue;
                }
                Branch from = s.getBranch();
                if (from.getId().equals(branch.getId())) {
                    skipped.add(sn + ": hiện đã thuộc chi nhánh này, không cần điều chỉnh");
                    continue;
                }
                if (!tryAddOnHand(v, from, -1, now)) {
                    skipped.add(sn + ": tồn khả dụng của " + from.getName() + " không đủ để trừ (đang giữ cho đơn)");
                    continue;
                }
                if (!tryAddOnHand(v, branch, 1, now))
                    throw new BusinessException("Không cộng được tồn tại " + branch.getName() + " cho " + sn + ".");
                s.setBranch(branch);
                histories.save(history(s, IN_STOCK, IN_STOCK, from, branch, st.getId(), user, now));
                it.setNote(append(it.getNote(), "Đã điều chỉnh: chuyển từ " + from.getName() + " về " + branch.getName()));
                it.setAdjustedBy(user);
                events.publishEvent(new StockChanged(v.getId(), from.getId()));
                events.publishEvent(new StockChanged(v.getId(), branch.getId()));
                relocated++;
            }
        }
        return new AdjustResult(relocated, writtenOff, skipped);
    }

    // ---------------------------------------------------------------- tiện ích

    private Stocktake require(String code) {
        return stocktakes.findByCode(code == null ? "" : code.trim())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên kiểm kê " + code));
    }

    private boolean tryAddOnHand(ProductVariant v, Branch b, int delta, Instant now) {
        if (balances.addOnHand(v.getId(), b.getId(), delta, now) == 1)
            return true;
        if (delta < 0)
            return false;
        try {
            InventoryBalance nb = new InventoryBalance(v, b, delta, 0);
            nb.setUpdatedAt(now);
            balances.saveAndFlush(nb);
            return true;
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Tồn kho của " + v.getSku() + " đang được cập nhật ở nơi khác, thử lại.");
        }
    }

    private AppUser user(String username) {
        List<AppUser> found = em.createQuery(
                "select u from AppUser u where u.username = :n and u.deletedAt is null", AppUser.class)
                .setParameter("n", username).getResultList();
        if (found.isEmpty())
            throw new BusinessException("Không xác định được người thao tác.");
        return found.get(0);
    }

    private static SerialStatusHistory history(SerialItem s, String from, String to, Branch fromBranch, Branch toBranch,
            Long refId, AppUser by, Instant at) {
        SerialStatusHistory h = new SerialStatusHistory();
        h.setSerial(s);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setFromBranch(fromBranch);
        h.setToBranch(toBranch);
        h.setRefType("STOCKTAKE");
        h.setRefId(refId);
        h.setChangedBy(by);
        h.setChangedAt(at);
        return h;
    }

    private static String append(String note, String more) {
        String n = note == null || note.isBlank() ? more : note + " · " + more;
        return n.length() > 255 ? n.substring(0, 255) : n;
    }

    private static String code(String prefix) {
        return prefix + LocalDateTime.now(ZoneId.systemDefault()).format(CODE_TIME) + "-"
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
