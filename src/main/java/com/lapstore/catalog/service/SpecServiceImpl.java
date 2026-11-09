package com.lapstore.catalog.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.AttributeDefinition;
import com.lapstore.catalog.domain.AttributeGroup;
import com.lapstore.catalog.domain.AttributeOption;
import com.lapstore.catalog.dto.SpecChoices;
import com.lapstore.catalog.dto.SpecChoices.Choice;
import com.lapstore.catalog.dto.SpecKind;
import com.lapstore.catalog.dto.SpecOptionRow;
import com.lapstore.catalog.repo.AttributeDefinitionRepository;
import com.lapstore.catalog.repo.AttributeOptionRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

@Service
class SpecServiceImpl implements SpecService {
    private static final String GROUP_NAME = "Cấu hình máy";
    private static final Pattern GB = Pattern.compile("(?i)^\\s*(\\d{1,6})\\s*(gb|g|tb|t)?\\s*$");
    private static final Map<SpecKind, int[]> DEFAULTS = Map.of(SpecKind.RAM, new int[] { 4, 8, 16, 32, 64 },
            SpecKind.STORAGE, new int[] { 256, 512, 1024, 2048 });

    private final AttributeDefinitionRepository defs;
    private final AttributeOptionRepository opts;

    @PersistenceContext
    private EntityManager em;

    SpecServiceImpl(AttributeDefinitionRepository defs, AttributeOptionRepository opts) {
        this.defs = defs;
        this.opts = opts;
    }

    // ------------------------------------------------------------ khởi tạo

    @Override
    @Transactional
    public void initialize() {
        AttributeGroup group = em.createQuery("select g from AttributeGroup g where g.name = :n", AttributeGroup.class)
                .setParameter("n", GROUP_NAME).getResultStream().findFirst().orElse(null);
        if (group == null) {
            group = new AttributeGroup();
            group.setName(GROUP_NAME);
            group.setSortOrder(1);
            em.persist(group);
        }
        int order = 1;
        for (SpecKind kind : SpecKind.values()) {
            boolean fresh = false;
            AttributeDefinition d = defs.findByCode(kind.code()).orElse(null);
            if (d == null) {
                d = new AttributeDefinition();
                d.setGroup(group);
                d.setCode(kind.code());
                d.setName(kind.label());
                d.setDataType(kind.numeric() ? "NUMBER" : "TEXT");
                d.setUnit(kind.numeric() ? "GB" : null);
                d.setAppliesTo("VARIANT");
                d.setFilterable(true);
                d.setComparable(true);
                d.setSortOrder(order);
                d.setActive(true);
                em.persist(d);
                fresh = true;
            }
            order++;
            Set<String> keys = new HashSet<>();
            for (AttributeOption o : opts.findByAttribute_Id(d.getId()))
                keys.add(keyOf(kind, o.getValue()));
            if (fresh && DEFAULTS.containsKey(kind))
                for (int n : DEFAULTS.get(kind))
                    if (keys.add(String.valueOf(n)))
                        createOption(d, kind, String.valueOf(n));
            // nạp các giá trị đang có ở biến thể mà danh mục chưa có
            for (Object v : em.createQuery("select distinct v." + kind.field() + " from ProductVariant v where v."
                    + kind.field() + " is not null", Object.class).getResultList()) {
                String value = kind.numeric() ? String.valueOf(((Number) v).intValue()) : clean(String.valueOf(v));
                if (value.isEmpty() || value.length() > 100)
                    continue;
                if (keys.add(keyOf(kind, value)))
                    createOption(d, kind, value);
            }
        }
    }

    // ------------------------------------------------------------ xem

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public List<SpecOptionRow> list(SpecKind kind) {
        AttributeDefinition d = definition(kind);
        Map<String, long[]> usage = usage(kind);
        List<SpecOptionRow> rows = new ArrayList<>();
        for (AttributeOption o : opts.findByAttribute_Id(d.getId())) {
            long[] u = usage.getOrDefault(keyOf(kind, o.getValue()), new long[2]);
            rows.add(new SpecOptionRow(o.getId(), o.getValue(), display(kind, o), u[1], u[0], !kind.numeric()));
        }
        rows.sort(order(kind));
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public SpecChoices choices() {
        return new SpecChoices(choicesOf(SpecKind.CPU), choicesOf(SpecKind.GPU), choicesOf(SpecKind.RAM),
                choicesOf(SpecKind.STORAGE));
    }

    private List<Choice> choicesOf(SpecKind kind) {
        List<AttributeOption> list = new ArrayList<>(opts.findByAttribute_Id(definition(kind).getId()));
        list.removeIf(o -> !o.isActive());
        List<Choice> out = new ArrayList<>();
        list.stream().map(o -> new SpecOptionRow(o.getId(), o.getValue(), display(kind, o), 0, 0, false))
                .sorted(order(kind)).forEach(r -> out.add(new Choice(r.value(), r.label())));
        return out;
    }

    // ------------------------------------------------------------ sửa

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    public void add(SpecKind kind, String raw) {
        String value = normalizeValue(kind, raw);
        AttributeDefinition d = definition(kind);
        String key = keyOf(kind, value);
        for (AttributeOption o : opts.findByAttribute_Id(d.getId()))
            if (keyOf(kind, o.getValue()).equals(key))
                throw new BusinessException("Đã có \"" + display(kind, o) + "\" trong danh sách " + kind.label()
                        + ", không tạo trùng.");
        createOption(d, kind, value);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    public void rename(long optionId, String newValue) {
        AttributeOption o = opts.findDetail(optionId).orElseThrow(() -> new NotFoundException("Không tìm thấy giá trị."));
        SpecKind kind = kindOf(o);
        if (kind.numeric())
            throw new BusinessException("RAM và ổ cứng là giá trị số, không đổi tên. Hãy thêm giá trị mới nếu cần.");
        String neu = normalizeValue(kind, newValue);
        String old = o.getValue();
        if (neu.equals(old))
            return;
        String oldKey = keyOf(kind, old);
        String newKey = keyOf(kind, neu);
        for (AttributeOption other : opts.findByAttribute_Id(o.getAttribute().getId()))
            if (!other.getId().equals(o.getId()) && keyOf(kind, other.getValue()).equals(newKey))
                throw new BusinessException("Đã có \"" + other.getDisplayName() + "\" trong danh sách " + kind.label()
                        + ". Không đổi tên trùng giá trị khác.");
        try {
            for (String raw : em.createQuery("select distinct v." + kind.field() + " from ProductVariant v where v."
                    + kind.field() + " is not null", String.class).getResultList())
                if (raw.equals(old) || keyOf(kind, raw).equals(oldKey))
                    em.createQuery("update ProductVariant v set v." + kind.field() + " = :n where v." + kind.field()
                            + " = :o").setParameter("n", neu).setParameter("o", raw).executeUpdate();
            o.setValue(neu);
            o.setDisplayName(neu);
            em.flush();
        } catch (PersistenceException e) {
            throw new BusinessException("Đổi tên làm hai biến thể của cùng một sản phẩm trùng nhau "
                    + "(cùng màu, CPU, RAM, ổ cứng). Hãy xử lý biến thể trước.");
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    public void delete(long optionId) {
        AttributeOption o = opts.findDetail(optionId).orElseThrow(() -> new NotFoundException("Không tìm thấy giá trị."));
        SpecKind kind = kindOf(o);
        long[] u = usage(kind).getOrDefault(keyOf(kind, o.getValue()), new long[2]);
        if (u[0] > 0)
            throw new BusinessException("\"" + display(kind, o) + "\" đang được " + u[1] + " sản phẩm (" + u[0]
                    + " biến thể) dùng nên không xóa được.");
        opts.delete(o);
    }

    @Override
    @Transactional
    public Choice resolve(SpecKind kind, String raw, boolean createIfMissing) {
        String value = normalizeValue(kind, raw);
        AttributeDefinition d = definition(kind);
        String key = keyOf(kind, value);
        for (AttributeOption o : opts.findByAttribute_Id(d.getId()))
            if (keyOf(kind, o.getValue()).equals(key))
                return new Choice(o.getValue(), display(kind, o));
        if (!createIfMissing)
            throw new BusinessException("\"" + raw.trim() + "\" chưa có trong danh sách " + kind.label()
                    + ". Hãy chọn giá trị có sẵn hoặc thêm ở mục Thuộc tính.");
        requireManage();
        AttributeOption o = createOption(d, kind, value);
        return new Choice(o.getValue(), display(kind, o));
    }

    // ------------------------------------------------------------ tiện ích

    private AttributeDefinition definition(SpecKind kind) {
        return defs.findByCode(kind.code()).orElseThrow(() -> new BusinessException(
                "Chưa khởi tạo thuộc tính " + kind.label() + ". Khởi động lại ứng dụng."));
    }

    private static SpecKind kindOf(AttributeOption o) {
        for (SpecKind k : SpecKind.values())
            if (k.code().equals(o.getAttribute().getCode()))
                return k;
        throw new BusinessException("Giá trị này không thuộc nhóm CPU/GPU/RAM/ổ cứng.");
    }

    private AttributeOption createOption(AttributeDefinition d, SpecKind kind, String value) {
        AttributeOption o = new AttributeOption();
        o.setAttribute(d);
        o.setValue(value);
        o.setDisplayName(kind.numeric() ? formatGb(Integer.parseInt(value)) : value);
        o.setSortOrder(kind.numeric() ? Integer.valueOf(value) : null);
        o.setActive(true);
        em.persist(o);
        return o;
    }

    /** Số sản phẩm / biến thể đang dùng từng giá trị, theo khóa chuẩn hóa. [0] = biến thể, [1] = sản phẩm. */
    private Map<String, long[]> usage(SpecKind kind) {
        String f = kind.field();
        Map<String, long[]> m = new HashMap<>();
        for (Object[] r : em.createQuery("select v." + f + ", count(v), count(distinct p.id) from ProductVariant v "
                + "join v.product p where p.deletedAt is null and v." + f + " is not null group by v." + f,
                Object[].class).getResultList()) {
            String key = kind.numeric() ? String.valueOf(((Number) r[0]).intValue()) : norm(String.valueOf(r[0]));
            long[] a = m.computeIfAbsent(key, k -> new long[2]);
            a[0] += ((Number) r[1]).longValue();
            a[1] += ((Number) r[2]).longValue();
        }
        return m;
    }

    private static String display(SpecKind kind, AttributeOption o) {
        return kind.numeric() ? formatGb(Integer.parseInt(o.getValue().trim())) : o.getDisplayName();
    }

    private static Comparator<SpecOptionRow> order(SpecKind kind) {
        return kind.numeric() ? Comparator.comparingInt(r -> Integer.parseInt(r.value().trim()))
                : Comparator.comparing(r -> r.label().toLowerCase(Locale.ROOT));
    }

    /** CPU/GPU: bỏ khoảng trắng thừa. RAM/ổ cứng: đọc số GB ("16", "16GB", "1TB" -> 1024). */
    static String normalizeValue(SpecKind kind, String raw) {
        if (raw == null)
            throw new BusinessException("Nhập giá trị " + kind.label() + ".");
        if (kind.numeric()) {
            Matcher m = GB.matcher(raw);
            if (!m.matches())
                throw new BusinessException("Giá trị " + kind.label() + " phải là số GB (VD 16, 512, 1TB).");
            long n = Long.parseLong(m.group(1));
            String unit = m.group(2) == null ? "" : m.group(2).toLowerCase(Locale.ROOT);
            if (unit.startsWith("t"))
                n *= 1024;
            if (n <= 0 || n > 99999)
                throw new BusinessException("Giá trị " + kind.label() + " không hợp lệ.");
            return String.valueOf(n);
        }
        String v = clean(raw);
        if (v.isEmpty() || v.length() > 100)
            throw new BusinessException("Giá trị " + kind.label() + " không được để trống và tối đa 100 ký tự.");
        return v;
    }

    private static String keyOf(SpecKind kind, String value) {
        return kind.numeric() ? value.trim() : norm(value);
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    /** Khóa chuẩn hóa: bỏ dấu, hạ chữ thường, bỏ mọi ký tự không phải chữ/số. */
    static String norm(String s) {
        return Normalizer.normalize(s.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static String formatGb(int n) {
        return n >= 1024 && n % 1024 == 0 ? (n / 1024) + " TB" : n + " GB";
    }

    private static void requireManage() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        boolean ok = a != null && a.getAuthorities().stream().anyMatch(x -> "PRODUCT_MANAGE".equals(x.getAuthority()));
        if (!ok)
            throw new BusinessException("Bạn không có quyền thêm giá trị thuộc tính mới (cần quyền PRODUCT_MANAGE).");
    }
}
