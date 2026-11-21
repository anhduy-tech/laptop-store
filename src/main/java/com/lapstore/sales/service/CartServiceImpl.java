package com.lapstore.sales.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.repo.ProductVariantRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.sales.domain.Cart;
import com.lapstore.sales.domain.CartItem;
import com.lapstore.sales.dto.CartItemView;
import com.lapstore.sales.dto.CartView;
import com.lapstore.sales.repo.CartItemRepository;
import com.lapstore.sales.repo.CartRepository;

@Service
class CartServiceImpl implements CartService {
    private static final int MAX_QTY_PER_ITEM = 10;
    private static final Duration CART_TTL = Duration.ofDays(7);

    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductVariantRepository variants;
    private final InventoryBalanceRepository balances;

    CartServiceImpl(CartRepository carts, CartItemRepository items,
                    ProductVariantRepository variants, InventoryBalanceRepository balances) {
        this.carts = carts;
        this.items = items;
        this.variants = variants;
        this.balances = balances;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView view(String cartToken) {
        if (cartToken == null || cartToken.isBlank()) {
            return CartView.empty();
        }
        Cart cart = carts.findByCartToken(cartToken).orElse(null);
        if (cart == null || !"ACTIVE".equals(cart.getStatus())) {
            return CartView.empty();
        }
        List<CartItem> rows = items.findByCart_IdOrderByIdAsc(cart.getId());
        if (rows.isEmpty()) {
            return CartView.empty();
        }
        List<CartItemView> views = rows.stream()
                .map(ci -> new CartItemView(
                        ci.getId(),
                        SalesSupport.variantDisplayName(ci.getVariant()),
                        ci.getVariant().getCurrentPrice().longValue(),
                        ci.getQuantity()))
                .toList();
        long total = views.stream().mapToLong(i -> i.unitPrice() * i.quantity()).sum();
        int count = views.stream().mapToInt(CartItemView::quantity).sum();
        return new CartView(views, total, count);
    }

    @Override
    @Transactional(readOnly = true)
    public int count(String cartToken) {
        if (cartToken == null || cartToken.isBlank()) {
            return 0;
        }
        return items.sumQuantityByCartToken(cartToken);
    }

    @Override
    @Transactional
    public void addItem(String cartToken, long variantId, int quantity) {
        if (cartToken == null || cartToken.isBlank()) {
            throw new BusinessException("Phiên giỏ hàng không hợp lệ.");
        }
        if (quantity < 1 || quantity > MAX_QTY_PER_ITEM) {
            throw new BusinessException("Số lượng phải từ 1 đến " + MAX_QTY_PER_ITEM + ".");
        }
        ProductVariant variant = variants.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình sản phẩm."));
        if (!"ACTIVE".equals(variant.getStatus()) || !"ACTIVE".equals(variant.getProduct().getStatus())) {
            throw new BusinessException("Sản phẩm hiện ngừng kinh doanh.");
        }

        Instant now = Instant.now();
        Cart cart = carts.findByCartToken(cartToken).orElseGet(() -> {
            Cart c = new Cart();
            c.setCartToken(cartToken);
            c.setStatus("ACTIVE");
            c.setExpiresAt(now.plus(CART_TTL));
            return carts.save(c);
        });
        if (!"ACTIVE".equals(cart.getStatus())) {
            cart.setStatus("ACTIVE");
        }
        cart.setExpiresAt(now.plus(CART_TTL));

        CartItem item = items.findByCart_IdAndVariant_Id(cart.getId(), variantId).orElse(null);
        int newQty = (item == null ? 0 : item.getQuantity()) + quantity;
        if (newQty > MAX_QTY_PER_ITEM) {
            throw new BusinessException("Mỗi sản phẩm trong giỏ tối đa " + MAX_QTY_PER_ITEM + " chiếc.");
        }
        long available = balances.sumAvailableByVariantId(variantId);
        if (available < newQty) {
            throw new BusinessException("Sản phẩm chỉ còn " + Math.max(0, available) + " máy khả dụng.");
        }

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
        }
        item.setQuantity(newQty);
        items.save(item);
    }

    @Override
    @Transactional
    public void removeItem(String cartToken, long cartItemId) {
        if (cartToken == null || cartToken.isBlank()) {
            return;
        }
        carts.findByCartToken(cartToken).ifPresent(cart ->
                items.findByIdAndCart_Id(cartItemId, cart.getId()).ifPresent(items::delete));
    }
}
