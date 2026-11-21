package com.lapstore.sales.service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.repo.SerialItemRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.dto.OrderDetail;
import com.lapstore.sales.dto.OrderItemDetail;
import com.lapstore.sales.dto.OrderLineView;
import com.lapstore.sales.dto.OrderRow;
import com.lapstore.sales.dto.OrderView;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.sales.repo.OrderItemRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
class OrderQueryServiceImpl implements OrderQueryService {
    private static final List<String> STATUSES = List.of(
            "NEW", "CONFIRMED", "PREPARING", "SHIPPING", "COMPLETED", "CANCELLED", "DRAFT");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final SerialItemRepository serials;
    private final StockHoldRepository holds;

    OrderQueryServiceImpl(CustomerOrderRepository orders, OrderItemRepository orderItems,
                          SerialItemRepository serials, StockHoldRepository holds) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.serials = serials;
        this.holds = holds;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> lookup(String code, String phone) {
        if (code == null || code.isBlank() || phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        return orders.findByOrderCodeIgnoreCaseAndCustomerPhone(code.trim(), phone.trim())
                .map(o -> {
                    List<OrderLineView> lines = orderItems.findByOrder_IdOrderByIdAsc(o.getId()).stream()
                            .map(oi -> new OrderLineView(
                                    SalesSupport.variantDisplayName(oi.getVariant()),
                                    oi.getUnitPrice().longValue(),
                                    oi.getQuantity()))
                            .toList();
                    return new OrderView(o.getOrderCode(), o.getStatus(), o.getTotalAmount().longValue(), lines);
                });
    }

    @Override
    public List<String> statuses() {
        return STATUSES;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderRow> search(String status, String keyword) {
        String st = status == null ? "" : status.trim();
        String kw = keyword == null ? "" : keyword.trim();
        return orders.search(st, kw).stream()
                .map(o -> new OrderRow(
                        o.getId(),
                        o.getOrderCode(),
                        o.getChannel(),
                        o.getCustomerName(),
                        o.getTotalAmount().longValue(),
                        o.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetail detail(long orderId) {
        CustomerOrder o = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn " + orderId));
        List<OrderItem> items = orderItems.findByOrder_IdOrderByIdAsc(orderId);
        List<Long> itemIds = items.stream().map(OrderItem::getId).toList();

        Map<Long, List<String>> soldSerialsByItem = itemIds.isEmpty() ? Map.of()
                : serials.findByOrderItem_IdIn(itemIds).stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getOrderItem().getId(),
                                Collectors.mapping(SerialItem::getSerialNumber, Collectors.toList())));

        List<StockHold> activeSerialHolds = holds.findByOrder_IdAndStatusOrderByIdAsc(orderId, "ACTIVE").stream()
                .filter(h -> "SERIAL".equals(h.getHoldType()) && h.getSerial() != null)
                .toList();

        List<OrderItemDetail> itemDetails = new ArrayList<>(items.size());
        for (OrderItem oi : items) {
            List<String> snList = new ArrayList<>(soldSerialsByItem.getOrDefault(oi.getId(), List.of()));
            for (StockHold h : activeSerialHolds) {
                boolean matchItem = h.getOrderItem() != null
                        ? h.getOrderItem().getId().equals(oi.getId())
                        : h.getVariant().getId().equals(oi.getVariant().getId());
                if (matchItem && !snList.contains(h.getSerial().getSerialNumber()) && snList.size() < oi.getQuantity()) {
                    snList.add(h.getSerial().getSerialNumber());
                }
            }
            itemDetails.add(new OrderItemDetail(
                    oi.getId(),
                    SalesSupport.variantDisplayName(oi.getVariant()),
                    oi.getUnitPrice().longValue(),
                    oi.getQuantity(),
                    snList));
        }

        return new OrderDetail(
                o.getId(),
                o.getOrderCode(),
                o.getStatus(),
                o.getCustomerName(),
                o.getCustomerPhone(),
                o.getShipAddressLine(),
                o.getTotalAmount().longValue(),
                itemDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] invoicePdf(long orderId) {
        CustomerOrder o = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn " + orderId));
        List<OrderItem> items = orderItems.findByOrder_IdOrderByIdAsc(orderId);
        NumberFormat vnd = NumberFormat.getInstance(Locale.of("vi", "VN"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("HOA DON BAN HANG - LAPSTORE", titleFont));
            doc.add(new Paragraph("Ma don: " + o.getOrderCode() + "   |   Trang thai: " + o.getStatus(), bodyFont));
            doc.add(new Paragraph("Ngay dat: " + DATE_FMT.format(o.getPlacedAt()), bodyFont));
            doc.add(new Paragraph("Khach hang: " + o.getCustomerName() + " - " + o.getCustomerPhone(), bodyFont));
            if (o.getShipAddressLine() != null && !o.getShipAddressLine().isBlank()) {
                doc.add(new Paragraph("Dia chi: " + o.getShipAddressLine(), bodyFont));
            }
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 5f, 1.2f, 2f, 2.2f });
            table.addCell(new Phrase("San pham", boldFont));
            table.addCell(new Phrase("SL", boldFont));
            table.addCell(new Phrase("Don gia", boldFont));
            table.addCell(new Phrase("Thanh tien", boldFont));

            for (OrderItem oi : items) {
                table.addCell(new Phrase(SalesSupport.variantDisplayName(oi.getVariant()), bodyFont));
                table.addCell(new Phrase(String.valueOf(oi.getQuantity()), bodyFont));
                table.addCell(new Phrase(vnd.format(oi.getUnitPrice().longValue()) + " d", bodyFont));
                table.addCell(new Phrase(vnd.format(oi.getLineTotal().longValue()) + " d", bodyFont));
            }
            doc.add(table);

            doc.add(new Paragraph(" "));
            if (o.getDiscountTotal().signum() > 0) {
                doc.add(new Paragraph("Giam gia: -" + vnd.format(o.getDiscountTotal().longValue()) + " d", bodyFont));
            }
            doc.add(new Paragraph("Tong thanh toan: " + vnd.format(o.getTotalAmount().longValue()) + " d", boldFont));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Không thể tạo PDF hóa đơn: " + e.getMessage());
        }
    }
}
