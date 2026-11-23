package com.lapstore.sales.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.dto.OrderDetail;
import com.lapstore.sales.dto.OrderItemDetail;
import com.lapstore.sales.dto.OrderLineView;
import com.lapstore.sales.dto.OrderRow;
import com.lapstore.sales.dto.OrderView;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.persistence.EntityManager;

/**
 * Truy vấn đơn (chỉ đọc). Khách tra cứu bằng mã đơn + SĐT (BR-08) nên {@link #lookup} không đòi đăng nhập và
 * không bao giờ cho biết "mã đơn có tồn tại nhưng SĐT sai". Đơn DRAFT (đơn quầy đang lên) không hiện ở đâu cả.
 */
@Service
class OrderQueryServiceImpl implements OrderQueryService {
    private static final String SALES = "hasAuthority('PORTAL_SALES')";
    private static final List<String> STATUSES = List.of("NEW", "CONFIRMED", "PREPARING", "SHIPPING", "COMPLETED",
            "CANCELLED");
    private static final int MAX_ROWS = 500;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE);

    private final EntityManager em;

    OrderQueryServiceImpl(EntityManager em) {
        this.em = em;
    }

    // ------------------------------------------------------------ khách tra cứu

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> lookup(String code, String phone) {
        String c = code == null ? "" : code.trim();
        String digits = digits(phone);
        if (c.isEmpty() || digits.isEmpty())
            return Optional.empty();
        List<CustomerOrder> found = em.createQuery("select o from CustomerOrder o where upper(o.orderCode) = :c "
                + "and o.status <> 'DRAFT'", CustomerOrder.class)
                .setParameter("c", c.toUpperCase(Locale.ROOT)).setMaxResults(1).getResultList();
        if (found.isEmpty() || !digits(found.get(0).getCustomerPhone()).equals(digits))
            return Optional.empty();
        CustomerOrder o = found.get(0);
        List<OrderLineView> lines = itemsOf(o.getId()).stream()
                .map(i -> new OrderLineView(nameOf(i), i.getUnitPrice().longValue(), i.getQuantity())).toList();
        return Optional.of(new OrderView(o.getOrderCode(), o.getStatus(), o.getTotalAmount().longValue(), lines));
    }

    // ------------------------------------------------------------ nhân viên

    @Override
    public List<String> statuses() {
        return STATUSES;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(SALES)
    public List<OrderRow> search(String status, String keyword) {
        StringBuilder jpql = new StringBuilder("select o from CustomerOrder o where o.status <> 'DRAFT'");
        Map<String, Object> params = new LinkedHashMap<>();
        if (status != null && STATUSES.contains(status)) {
            jpql.append(" and o.status = :st");
            params.put("st", status);
        }
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) { // Oracle coi '' là NULL nên chỉ ghép điều kiện khi có từ khóa
            jpql.append(" and (lower(o.orderCode) like :kw escape '!' or o.customerPhone like :kw escape '!'"
                    + " or lower(o.customerName) like :kw escape '!')");
            params.put("kw", "%" + escapeLike(kw.toLowerCase(Locale.ROOT)) + "%");
        }
        jpql.append(" order by o.placedAt desc, o.id desc");
        var q = em.createQuery(jpql.toString(), CustomerOrder.class).setMaxResults(MAX_ROWS);
        params.forEach(q::setParameter);
        return q.getResultList().stream().map(o -> new OrderRow(o.getId(), o.getOrderCode(), o.getChannel(),
                o.getCustomerName(), o.getTotalAmount().longValue(), o.getStatus())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(SALES)
    public OrderDetail detail(long orderId) {
        CustomerOrder o = load(orderId);
        Map<Long, List<String>> serials = serialsByItem(orderId);
        List<OrderItemDetail> items = itemsOf(orderId).stream()
                .map(i -> new OrderItemDetail(i.getId(), nameOf(i), i.getUnitPrice().longValue(), i.getQuantity(),
                        serials.getOrDefault(i.getId(), List.of())))
                .toList();
        String address = "PICKUP".equals(o.getFulfillmentMethod()) ? "Nhận tại cửa hàng"
                : (o.getShipAddressLine() == null || o.getShipAddressLine().isBlank() ? null : o.getShipAddressLine());
        return new OrderDetail(o.getId(), o.getOrderCode(), o.getStatus(), o.getCustomerName(), o.getCustomerPhone(),
                address, o.getTotalAmount().longValue(), items);
    }

    // ------------------------------------------------------------ hóa đơn PDF

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(SALES)
    public byte[] invoicePdf(long orderId) {
        CustomerOrder o = load(orderId);
        if (!"COMPLETED".equals(o.getStatus()))
            throw new BusinessException("Chỉ xuất hóa đơn cho đơn đã hoàn tất.");
        List<OrderItem> items = itemsOf(orderId);
        Map<Long, List<String>> serials = serialsByItem(orderId);
        try {
            BaseFont regular = font("fonts/DejaVuSans.ttf");
            BaseFont bold = font("fonts/DejaVuSans-Bold.ttf");
            Font title = new Font(bold, 18), strong = new Font(bold, 10), normal = new Font(regular, 10),
                    small = new Font(regular, 8.5f);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph head = new Paragraph("LAPSTORE", new Font(bold, 14));
            doc.add(head);
            Paragraph t = new Paragraph("HÓA ĐƠN BÁN HÀNG", title);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingBefore(6);
            t.setSpacingAfter(10);
            doc.add(t);

            Instant when = o.getCompletedAt() != null ? o.getCompletedAt() : o.getPlacedAt();
            doc.add(new Paragraph("Mã đơn: " + o.getOrderCode() + "    Ngày: " + TIME.format(when), normal));
            doc.add(new Paragraph("Khách hàng: " + o.getCustomerName()
                    + ("-".equals(o.getCustomerPhone()) ? "" : " · " + o.getCustomerPhone()), normal));
            Paragraph gap = new Paragraph(" ", small);
            doc.add(gap);

            PdfPTable table = new PdfPTable(new float[] { 0.6f, 4.4f, 0.8f, 1.7f, 1.5f, 1.9f });
            table.setWidthPercentage(100);
            for (String h : new String[] { "STT", "Sản phẩm / Serial", "SL", "Đơn giá", "Giảm", "Thành tiền" })
                table.addCell(cell(h, strong, Element.ALIGN_CENTER, true));
            int n = 0;
            for (OrderItem i : items) {
                n++;
                String sn = String.join(", ", serials.getOrDefault(i.getId(), List.of()));
                PdfPCell name = cell(nameOf(i), normal, Element.ALIGN_LEFT, false);
                if (!sn.isEmpty())
                    name.addElement(new Phrase("SN: " + sn, small));
                table.addCell(cell(String.valueOf(n), normal, Element.ALIGN_CENTER, false));
                table.addCell(name);
                table.addCell(cell(String.valueOf(i.getQuantity()), normal, Element.ALIGN_CENTER, false));
                table.addCell(cell(money(i.getUnitPrice()), normal, Element.ALIGN_RIGHT, false));
                table.addCell(cell(i.getLineDiscount().signum() > 0 ? "-" + money(i.getLineDiscount()) : "—", normal,
                        Element.ALIGN_RIGHT, false));
                table.addCell(cell(money(i.getLineTotal()), normal, Element.ALIGN_RIGHT, false));
            }
            doc.add(table);

            PdfPTable sum = new PdfPTable(new float[] { 6f, 2.5f });
            sum.setWidthPercentage(60);
            sum.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sum.setSpacingBefore(10);
            sumRow(sum, "Tạm tính", money(o.getSubtotal()), normal);
            if (o.getDiscountTotal().signum() > 0)
                sumRow(sum, "Giảm giá", "-" + money(o.getDiscountTotal()), normal);
            if (o.getShippingFee().signum() > 0)
                sumRow(sum, "Phí vận chuyển", money(o.getShippingFee()), normal);
            sumRow(sum, "Tổng thanh toán", money(o.getTotalAmount()), strong);
            doc.add(sum);

            Paragraph thanks = new Paragraph("Cảm ơn quý khách. Giữ hóa đơn và serial để được bảo hành.", small);
            thanks.setSpacingBefore(24);
            thanks.setAlignment(Element.ALIGN_CENTER);
            doc.add(thanks);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Không tạo được hóa đơn PDF", e);
        }
    }

    // ------------------------------------------------------------ nội bộ

    private CustomerOrder load(long id) {
        CustomerOrder o = em.find(CustomerOrder.class, id);
        if (o == null || "DRAFT".equals(o.getStatus()))
            throw new NotFoundException("Không tìm thấy đơn " + id);
        return o;
    }

    private List<OrderItem> itemsOf(Long orderId) {
        return em.createQuery("select i from OrderItem i join fetch i.variant v join fetch v.product "
                + "where i.order.id = :id order by i.id", OrderItem.class).setParameter("id", orderId)
                .getResultList();
    }

    private Map<Long, List<String>> serialsByItem(Long orderId) {
        Map<Long, List<String>> map = new LinkedHashMap<>();
        for (SerialItem s : em.createQuery("select s from SerialItem s join fetch s.orderItem oi "
                + "where oi.order.id = :id order by s.serialNumber", SerialItem.class)
                .setParameter("id", orderId).getResultList())
            map.computeIfAbsent(s.getOrderItem().getId(), k -> new ArrayList<>()).add(s.getSerialNumber());
        return map;
    }

    private static String nameOf(OrderItem i) {
        return i.getVariant().getProduct().getName() + " · " + i.getVariant().getVariantName();
    }

    private static String digits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String money(BigDecimal v) {
        return NumberFormat.getIntegerInstance(Locale.GERMANY).format(v.longValue()) + " ₫"; // dấu chấm ngăn cách nghìn
    }

    private static BaseFont font(String path) throws IOException, DocumentException {
        byte[] bytes = new ClassPathResource(path).getContentAsByteArray();
        return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
    }

    private static PdfPCell cell(String text, Font f, int align, boolean header) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(align);
        c.setPadding(5);
        if (header)
            c.setBackgroundColor(new java.awt.Color(0xEE, 0xF2, 0xF7));
        return c;
    }

    private static void sumRow(PdfPTable t, String label, String value, Font f) {
        PdfPCell a = new PdfPCell(new Phrase(label, f));
        PdfPCell b = new PdfPCell(new Phrase(value, f));
        a.setBorder(0);
        b.setBorder(0);
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(a);
        t.addCell(b);
    }
}
