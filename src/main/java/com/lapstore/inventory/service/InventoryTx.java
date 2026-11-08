package com.lapstore.inventory.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.dto.ResolvedVariant;
import com.lapstore.catalog.dto.VariantSpec;
import com.lapstore.catalog.service.VariantProvisioningService;
import com.lapstore.common.dto.IdName;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.common.exception.SerialHeldException;
import com.lapstore.common.exception.SerialNotAvailableException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.domain.GoodsReceipt;
import com.lapstore.inventory.domain.GoodsReceiptItem;
import com.lapstore.inventory.domain.InventoryBalance;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.SerialStatusHistory;
import com.lapstore.inventory.domain.StockTransfer;
import com.lapstore.inventory.domain.StockTransferItem;
import com.lapstore.inventory.domain.Supplier;
import com.lapstore.inventory.dto.ReceiptResult;
import com.lapstore.inventory.dto.StockReceiptRequest;
import com.lapstore.inventory.dto.StockReceiptRequest.ProductLine;
import com.lapstore.inventory.dto.StockReceiptRequest.VariantLine;
import com.lapstore.inventory.dto.StockTransferRequest;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.BranchRepository;
import com.lapstore.inventory.repo.GoodsReceiptItemRepository;
import com.lapstore.inventory.repo.GoodsReceiptRepository;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.SerialItemRepository;
import com.lapstore.inventory.repo.SerialStatusHistoryRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.inventory.repo.StockTransferItemRepository;
import com.lapstore.inventory.repo.StockTransferRepository;
import com.lapstore.inventory.repo.SupplierRepository;
import com.lapstore.staff.domain.AppUser;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Lớp TRONG của nhập/chuyển kho: mọi thay đổi DB của MỘT thao tác nằm trong MỘT transaction.
 * Chuyển kho phải được gọi từ InventoryServiceImpl khi đã giữ KeyedLock "serial:..." — đừng gọi trực tiếp.
 */
@Service
public class InventoryTx {
    static final int MAX_SERIALS = 500;

    private static final String IN_STOCK = "IN_STOCK";
    private static final String IN_TRANSIT = "IN_TRANSIT";
    private static final String ACTIVE = "ACTIVE";
    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SupplierRepository suppliers;
    private final BranchRepository branches;
    private final GoodsReceiptRepository receipts;
    private final GoodsReceiptItemRepository receiptItems;
    private final SerialItemRepository serials;
    private final SerialStatusHistoryRepository histories;
    private final InventoryBalanceRepository balances;
    private final StockHoldRepository holds;
    private final StockTransferRepository transfers;
    private final StockTransferItemRepository transferItems;
    private final ApplicationEventPublisher events;
    private final VariantProvisioningService catalog;

    @PersistenceContext
    private EntityManager em;

    InventoryTx(SupplierRepository suppliers, BranchRepository branches, GoodsReceiptRepository receipts,
            GoodsReceiptItemRepository receiptItems, SerialItemRepository serials,
            SerialStatusHistoryRepository histories, InventoryBalanceRepository balances,
            StockHoldRepository holds, StockTransferRepository transfers,
            StockTransferItemRepository transferItems, ApplicationEventPublisher events,
            VariantProvisioningService catalog) {
        this.suppliers = suppliers;
        this.branches = branches;
        this.receipts = receipts;
        this.receiptItems = receiptItems;
        this.serials = serials;
        this.histories = histories;
        this.balances = balances;
        this.holds = holds;
        this.transfers = transfers;
        this.transferItems = transferItems;
        this.events = events;
        this.catalog = catalog;
    }

    // ---------------------------------------------------------------- danh mục

    @Transactional(readOnly = true)
    public List<IdName> suppliers() {
        return suppliers.findByActiveTrueOrderByNameAsc().stream()
                .map(s -> new IdName(s.getId(), s.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<IdName> branches() {
        return branches.findByActiveTrueOrderByNameAsc().stream()
                .map(b -> new IdName(b.getId(), b.getName())).toList();
    }

    // ---------------------------------------------------------------- nhập kho

    /** Dòng đã làm sạch, sẵn sàng xử lý: thuộc sản phẩm thứ blockNo của phiếu. */
    private record Pending(int blockNo, ProductLine block, VariantLine row, String where, List<String> serials) {
    }

    /**
     * Nhập kho một lần: kiểm tra toàn bộ đầu vào trước, rồi tạo sản phẩm/biến thể mới (nếu có), sinh serial
     * IN_STOCK, cộng on_hand và ghi serial_status_history. Biến thể không có serial bị bỏ qua.
     */
    @Transactional
    public ReceiptResult createReceipt(StockReceiptRequest r, String username) {
        Instant now = Instant.now();
        AppUser user = user(username);
        Supplier supplier = r.supplierId() == null ? null : em.find(Supplier.class, r.supplierId());
        if (supplier == null || !supplier.isActive())
            throw new BusinessException("Nhà cung cấp không hợp lệ hoặc đã ngừng hoạt động.");
        Branch branch = activeBranch(r.branchId(), "nhận hàng");

        // 1) Làm sạch và kiểm tra cú pháp (chưa đụng DB)
        List<ProductLine> blocks = r.products() == null ? List.<ProductLine>of()
                : r.products().stream().filter(Objects::nonNull).toList();
        List<Pending> pending = new ArrayList<>();
        Set<String> allSerials = new LinkedHashSet<>();
        for (int i = 0; i < blocks.size(); i++) {
            ProductLine block = blocks.get(i);
            List<VariantLine> rows = block.variants() == null ? List.<VariantLine>of()
                    : block.variants().stream().filter(Objects::nonNull).toList();
            for (int j = 0; j < rows.size(); j++) {
                VariantLine row = rows.get(j);
                List<String> sns = row.serials() == null ? List.<String>of() : row.serials().stream()
                        .filter(x -> x != null && !x.isBlank()).map(String::trim).toList();
                if (sns.isEmpty())
                    continue;
                String where = "Sản phẩm " + (i + 1) + ", cấu hình " + (j + 1) + ": ";
                if (row.unitCost() == null || row.unitCost().signum() <= 0)
                    throw new BusinessException(where + "nhập giá vốn lớn hơn 0.");
                for (String sn : sns) {
                    if (sn.length() > 60)
                        throw new BusinessException(where + "serial \"" + sn + "\" dài quá 60 ký tự.");
                    if (!allSerials.add(sn))
                        throw new BusinessException("Serial " + sn + " bị lặp trong phiếu.");
                }
                pending.add(new Pending(i, block, row, where, sns));
            }
        }
        if (pending.isEmpty())
            throw new BusinessException("Chưa có serial nào. Mỗi máy nhập cần một serial ở ô Serial của cấu hình tương ứng.");
        if (allSerials.size() > MAX_SERIALS)
            throw new BusinessException("Mỗi phiếu tối đa " + MAX_SERIALS + " serial.");
        List<String> existing = serials.findExistingSerialNumbers(allSerials);
        if (!existing.isEmpty())
            throw new BusinessException("Serial đã tồn tại trong hệ thống: " + String.join(", ", existing) + ".");

        // 2) Phiếu nhập
        String code = code("PN");
        GoodsReceipt gr = new GoodsReceipt();
        gr.setCode(code);
        gr.setSupplier(supplier);
        gr.setBranch(branch);
        gr.setReceivedBy(user);
        gr.setReceivedAt(now);
        gr.setStatus("CONFIRMED");
        gr.setNote(r.note());
        receipts.save(gr);

        // 3) Sản phẩm/biến thể (tạo khi chưa có), dòng phiếu, serial
        Map<Integer, Long> productIds = new HashMap<>();
        Map<Long, ProductVariant> variantRefs = new LinkedHashMap<>();
        Map<Long, Integer> qtyByVariant = new LinkedHashMap<>();
        List<SerialItem> created = new ArrayList<>();
        int newProducts = 0;
        int newVariants = 0;
        String reason = "Giá bán khởi tạo khi nhập hàng (phiếu " + code + ")";
        for (Pending p : pending) {
            Long productId = p.block().productId();
            if (productId == null) {
                productId = productIds.get(p.blockNo());
                if (productId == null) {
                    if (p.block().newProduct() == null)
                        throw new BusinessException(p.where() + "chọn sản phẩm có sẵn hoặc nhập thông tin sản phẩm mới.");
                    productId = catalog.createProduct(p.block().newProduct());
                    productIds.put(p.blockNo(), productId);
                    newProducts++;
                }
            }
            VariantLine row = p.row();
            ResolvedVariant rv;
            try {
                rv = catalog.findOrCreateVariant(new VariantSpec(productId, row.colorId(), row.cpu(),
                        p.block().gpu(), row.ramGb(), row.storageGb(), row.sellPrice()), reason,
                        username);
            } catch (BusinessException e) {
                throw new BusinessException(p.where() + e.getMessage());
            }
            if (rv.created())
                newVariants++;
            ProductVariant v = variantRefs.computeIfAbsent(rv.variantId(),
                    id -> em.getReference(ProductVariant.class, id));
            GoodsReceiptItem item = new GoodsReceiptItem();
            item.setReceipt(gr);
            item.setVariant(v);
            item.setQuantity(p.serials().size());
            item.setUnitCost(row.unitCost());
            receiptItems.save(item);
            qtyByVariant.merge(rv.variantId(), p.serials().size(), Integer::sum);
            for (String sn : p.serials()) {
                SerialItem s = new SerialItem();
                s.setVariant(v);
                s.setBranch(branch);
                s.setSerialNumber(sn);
                s.setStatus(IN_STOCK);
                s.setGoodsReceiptItem(item);
                s.setCostPrice(row.unitCost());
                created.add(s);
            }
        }

        try {
            serials.saveAllAndFlush(created); // UNIQUE(serial_number) là lưới an toàn cuối (BR-01)
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Có serial vừa được nhập ở nơi khác (trùng). Kiểm tra lại danh sách serial.");
        }
        for (SerialItem s : created)
            histories.save(history(s, null, IN_STOCK, null, branch, "RECEIPT", gr.getId(), user, now));
        for (var e : qtyByVariant.entrySet()) {
            addOnHand(variantRefs.get(e.getKey()), branch, e.getValue(), now);
            events.publishEvent(new StockChanged(e.getKey(), branch.getId()));
        }
        return new ReceiptResult(code, created.size(), newProducts, newVariants);
    }

    // ---------------------------------------------------------------- chuyển kho

    /** Xuất máy khỏi chi nhánh nguồn: serial IN_STOCK -> IN_TRANSIT, trừ on_hand nguồn, phiếu IN_TRANSIT. */
    @Transactional
    public String transfer(StockTransferRequest r, String username) {
        Instant now = Instant.now();
        AppUser user = user(username);
        if (r.fromBranchId() == null || r.toBranchId() == null || r.fromBranchId().equals(r.toBranchId()))
            throw new BusinessException("Chi nhánh nguồn và đích phải khác nhau.");
        Branch from = activeBranch(r.fromBranchId(), "nguồn");
        Branch to = activeBranch(r.toBranchId(), "đích");

        List<SerialItem> found = serials.findBySerialNumberIn(r.serials());
        Set<String> foundSn = found.stream().map(SerialItem::getSerialNumber).collect(Collectors.toSet());
        List<String> missing = r.serials().stream().filter(x -> !foundSn.contains(x)).toList();
        if (!missing.isEmpty())
            throw new BusinessException("Không tìm thấy serial: " + String.join(", ", missing) + ".");
        for (SerialItem s : found) {
            String sn = s.getSerialNumber();
            if (!IN_STOCK.equals(s.getStatus()))
                throw new SerialNotAvailableException(sn, s.getStatus());
            if (!s.getBranch().getId().equals(from.getId()))
                throw new BusinessException("Máy " + sn + " không nằm ở chi nhánh nguồn.");
            if (holds.existsBySerial_IdAndStatus(s.getId(), ACTIVE))
                throw new SerialHeldException(sn);
        }

        StockTransfer t = new StockTransfer();
        t.setCode(code("CK"));
        t.setFromBranch(from);
        t.setToBranch(to);
        t.setStatus(IN_TRANSIT);
        t.setShippedBy(user);
        t.setShippedAt(now);
        transfers.save(t);

        Map<Long, List<SerialItem>> byVariant = found.stream()
                .collect(Collectors.groupingBy(s -> s.getVariant().getId(), LinkedHashMap::new, Collectors.toList()));
        for (SerialItem s : found) {
            StockTransferItem it = new StockTransferItem();
            it.setTransfer(t);
            it.setSerial(s);
            transferItems.save(it);
            s.setStatus(IN_TRANSIT); // branch giữ nguyên là nguồn tới khi bên đích nhận
            histories.save(history(s, IN_STOCK, IN_TRANSIT, from, to, "TRANSFER", t.getId(), user, now));
        }
        for (var e : byVariant.entrySet()) {
            ProductVariant v = e.getValue().get(0).getVariant();
            if (!tryAddOnHand(v, from, -e.getValue().size(), now))
                throw new BusinessException("Tồn khả dụng của " + v.getSku() + " tại chi nhánh nguồn không đủ để chuyển "
                        + e.getValue().size() + " máy (đang có máy bị giữ cho đơn).");
            events.publishEvent(new StockChanged(v.getId(), from.getId()));
        }
        return t.getCode();
    }

    /** Serial của một phiếu chuyển, để service ngoài lấy khóa trước khi nhận hàng. */
    @Transactional(readOnly = true)
    public List<String> transferSerials(String transferCode) {
        StockTransfer t = transfers.findByCode(transferCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chuyển " + transferCode));
        return transferItems.findWithSerial(t.getId()).stream().map(i -> i.getSerial().getSerialNumber()).toList();
    }

    /** Chi nhánh đích nhận hàng: serial IN_TRANSIT -> IN_STOCK tại đích, cộng on_hand đích, phiếu RECEIVED. */
    @Transactional
    public void receiveTransfer(String transferCode, String username) {
        Instant now = Instant.now();
        AppUser user = user(username);
        StockTransfer t = transfers.findByCode(transferCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chuyển " + transferCode));
        if (!IN_TRANSIT.equals(t.getStatus()))
            throw new BusinessException("Phiếu " + transferCode + " không ở trạng thái đang vận chuyển.");
        Branch to = t.getToBranch();
        Branch from = t.getFromBranch();

        List<StockTransferItem> items = transferItems.findWithSerial(t.getId());
        Map<Long, List<SerialItem>> byVariant = new LinkedHashMap<>();
        for (StockTransferItem it : items) {
            SerialItem s = it.getSerial();
            if (!IN_TRANSIT.equals(s.getStatus()))
                throw new SerialNotAvailableException(s.getSerialNumber(), s.getStatus());
            s.setStatus(IN_STOCK);
            s.setBranch(to);
            it.setReceivedOk(true);
            histories.save(history(s, IN_TRANSIT, IN_STOCK, from, to, "TRANSFER", t.getId(), user, now));
            byVariant.computeIfAbsent(s.getVariant().getId(), k -> new ArrayList<>()).add(s);
        }
        for (var e : byVariant.entrySet()) {
            ProductVariant v = e.getValue().get(0).getVariant();
            addOnHand(v, to, e.getValue().size(), now);
            events.publishEvent(new StockChanged(v.getId(), to.getId()));
        }
        t.setStatus("RECEIVED");
        t.setReceivedBy(user);
        t.setReceivedAt(now);
    }

    // ---------------------------------------------------------------- tiện ích

    /** Cộng/trừ on_hand; chưa có dòng tồn thì tạo mới (chỉ khi delta > 0). */
    private void addOnHand(ProductVariant v, Branch b, int delta, Instant now) {
        if (!tryAddOnHand(v, b, delta, now))
            throw new BusinessException("Không cập nhật được tồn kho của " + v.getSku() + ".");
    }

    private boolean tryAddOnHand(ProductVariant v, Branch b, int delta, Instant now) {
        if (balances.addOnHand(v.getId(), b.getId(), delta, now) == 1)
            return true;
        if (delta < 0)
            return false; // không đủ tồn khả dụng
        try {
            InventoryBalance nb = new InventoryBalance(v, b, delta, 0);
            nb.setUpdatedAt(now);
            balances.saveAndFlush(nb);
            return true;
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Tồn kho của " + v.getSku() + " đang được cập nhật ở nơi khác, vui lòng thử lại.");
        }
    }

    private Branch activeBranch(Long id, String role) {
        Branch b = id == null ? null : em.find(Branch.class, id);
        if (b == null || !b.isActive())
            throw new BusinessException("Chi nhánh " + role + " không hợp lệ hoặc đã ngừng hoạt động.");
        return b;
    }

    private AppUser user(String username) {
        List<AppUser> found = em.createQuery(
                "select u from AppUser u where u.username = :n and u.deletedAt is null", AppUser.class)
                .setParameter("n", username).getResultList();
        if (found.isEmpty())
            throw new BusinessException("Không xác định được người thao tác.");
        return found.get(0);
    }

    private static SerialStatusHistory history(SerialItem s, String fromStatus, String toStatus, Branch fromBranch,
            Branch toBranch, String refType, Long refId, AppUser by, Instant at) {
        SerialStatusHistory h = new SerialStatusHistory();
        h.setSerial(s);
        h.setFromStatus(fromStatus);
        h.setToStatus(toStatus);
        h.setFromBranch(fromBranch);
        h.setToBranch(toBranch);
        h.setRefType(refType);
        h.setRefId(refId);
        h.setChangedBy(by);
        h.setChangedAt(at);
        return h;
    }

    /** Mã phiếu dạng PN20261108-143015-482 (UNIQUE ở DB là lưới an toàn nếu trùng). */
    private static String code(String prefix) {
        return prefix + LocalDateTime.now(ZoneId.systemDefault()).format(CODE_TIME) + "-"
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
