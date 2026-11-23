package com.lapstore.sales.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.sales.domain.Cart;
import com.lapstore.sales.domain.CartItem;
import com.lapstore.sales.dto.CartItemView;
import com.lapstore.sales.dto.CartView;

import jakarta.persistence.EntityManager;

/**
 * Giỏ hàng của khách vãng lai (không cần đăng nhập), định danh bằng token UUID trong HttpSession.
 * Giỏ không giữ hàng: chỉ lúc đặt đơn mới giữ số lượng (OnlineOrderService). Giá hiển thị là giá hiệu lực
 * hiện tại, giá thật được chốt lúc đặt đơn (BR-05, BR-06).
 */
@Service
class CartServiceImpl implements CartService {
    private static final int MAX_PER_ITEM = 10;
    private static final int MAX_LINES = 20;
    private static final Pattern TOKEN = Pattern.compile("[0-9a-fA-F-]{36}");

    private final EntityManager em;
    private final PriceHistoryRepository prices;

    CartServiceImpl(EntityManager em, PriceHistoryRepository prices) {
        this.em = em;
        this.prices = prices;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView view(String cartToken) {
        Cart cart = find(cartToken);
        if (cart == null)
            return CartView.empty();
        Instant now = Instant.now();
        List<CartItemView> items = itemsOf(cart).stream().map(i -> {
            ProductVariant v = i.getVariant();
            return new CartItemView(i.getId(), v.getProduct().getName() + " · " + v.getVariantName(),
                    price(v, now), i.getQuantity());
        }).toList();
        long total = items.stream().mapToLong(i -> i.unitPrice() * i.quantity()).sum();
        int count = items.stream().mapToInt(CartItemView::quantity).sum();
        return new CartView(items, total, count);
    }

    @Override
    @Transactional(readOnly = true)
    public int count(String cartToken) {
        Cart cart = find(cartToken);
        if (cart == null)
            return 0;
        Long n = em.createQuery("select coalesce(sum(i.quantity), 0) from CartItem i where i.cart.id = :id", Long.class)
                .setParameter("id", cart.getId()).getSingleResult();
        return n.intValue();
    }

    @Override
    @Transactional
    public void addItem(String cartToken, long variantId, int quantity) {
        if (quantity < 1)
            throw new BusinessException("Số lượng phải từ 1 trở lên.");
        ProductVariant v = em.find(ProductVariant.class, variantId);
        if (v == null || !"ACTIVE".equals(v.getStatus()))
            throw new BusinessException("Sản phẩm này hiện không bán.");
        Cart cart = find(cartToken);
        if (cart == null) {
            if (cartToken == null || !TOKEN.matcher(cartToken).matches())
                throw new BusinessException("Phiên giỏ hàng không hợp lệ.");
            cart = new Cart();
            cart.setCartToken(cartToken);
            cart.setStatus("ACTIVE");
            em.persist(cart);
        }
        cart.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
        List<CartItem> existing = em.createQuery("select i from CartItem i where i.cart.id = :c and i.variant.id = :v",
                CartItem.class).setParameter("c", cart.getId()).setParameter("v", variantId).getResultList();
        if (!existing.isEmpty()) {
            CartItem i = existing.get(0);
            i.setQuantity(Math.min(MAX_PER_ITEM, i.getQuantity() + quantity));
            return;
        }
        if (itemsOf(cart).size() >= MAX_LINES)
            throw new BusinessException("Giỏ hàng tối đa " + MAX_LINES + " dòng.");
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setVariant(v);
        item.setQuantity(Math.min(MAX_PER_ITEM, quantity));
        em.persist(item);
    }

    @Override
    @Transactional
    public void removeItem(String cartToken, long cartItemId) {
        Cart cart = find(cartToken);
        if (cart == null)
            return;
        CartItem i = em.find(CartItem.class, cartItemId);
        if (i != null && i.getCart().getId().equals(cart.getId()))
            em.remove(i); // chỉ xóa dòng của đúng giỏ này
    }

    // ------------------------------------------------------------------ nội bộ

    private Cart find(String token) {
        if (token == null || !TOKEN.matcher(token).matches())
            return null;
        List<Cart> l = em.createQuery("select c from Cart c where c.cartToken = :t and c.status = 'ACTIVE'", Cart.class)
                .setParameter("t", token).setMaxResults(1).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    private List<CartItem> itemsOf(Cart cart) {
        return em.createQuery("select i from CartItem i join fetch i.variant v join fetch v.product "
                + "where i.cart.id = :id order by i.id", CartItem.class).setParameter("id", cart.getId())
                .getResultList();
    }

    private long price(ProductVariant v, Instant now) {
        List<PriceHistory> l = prices.findEffective(v.getId(), now);
        if (!l.isEmpty())
            return l.get(0).getPrice().longValue();
        return v.getCurrentPrice() == null ? 0 : v.getCurrentPrice().longValue();
    }
}
