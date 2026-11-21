package com.lapstore.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.Product;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.catalog.repo.ProductVariantRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.domain.InventoryBalance;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.repo.BranchRepository;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.SerialItemRepository;
import com.lapstore.inventory.repo.SerialStatusHistoryRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.sales.domain.Cart;
import com.lapstore.sales.domain.CartItem;
import com.lapstore.sales.domain.Customer;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.domain.Voucher;
import com.lapstore.sales.domain.VoucherRedemption;
import com.lapstore.sales.dto.CartView;
import com.lapstore.sales.dto.CheckoutRequest;
import com.lapstore.sales.dto.CounterOrderView;
import com.lapstore.sales.repo.CartItemRepository;
import com.lapstore.sales.repo.CartRepository;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.sales.repo.CustomerRepository;
import com.lapstore.sales.repo.OrderDiscountLineRepository;
import com.lapstore.sales.repo.OrderItemRepository;
import com.lapstore.sales.repo.OrderStatusHistoryRepository;
import com.lapstore.sales.repo.PaymentRepository;
import com.lapstore.sales.repo.VoucherRedemptionRepository;
import com.lapstore.sales.repo.VoucherRepository;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.repo.AppUserRepository;

class SalesServicesTest {

    @Test
    void cartAddAndViewComputesTotalAndChecksStock() {
        CartRepository carts = mock(CartRepository.class);
        CartItemRepository items = mock(CartItemRepository.class);
        ProductVariantRepository variants = mock(ProductVariantRepository.class);
        InventoryBalanceRepository balances = mock(InventoryBalanceRepository.class);
        CartServiceImpl service = new CartServiceImpl(carts, items, variants, balances);

        ProductVariant v = sampleVariant(1L, "ThinkPad X1", "i7 / 16GB", 30_000_000L);
        Cart cart = new Cart();
        cart.setCartToken("tok-1");
        cart.setStatus("ACTIVE");

        when(variants.findById(1L)).thenReturn(Optional.of(v));
        when(carts.findByCartToken("tok-1")).thenReturn(Optional.of(cart));
        when(balances.sumAvailableByVariantId(1L)).thenReturn(3L);

        service.addItem("tok-1", 1L, 2);
        verify(items).save(any(CartItem.class));

        CartItem ci = new CartItem();
        ci.setCart(cart);
        ci.setVariant(v);
        ci.setQuantity(2);
        when(items.findByCart_IdOrderByIdAsc(any())).thenReturn(List.of(ci));

        CartView view = service.view("tok-1");
        assertThat(view.count()).isEqualTo(2);
        assertThat(view.total()).isEqualTo(60_000_000L);

        assertThatThrownBy(() -> service.addItem("tok-1", 1L, 5))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void onlineOrderPlacesOrderReservesStockAndVoucher() {
        CartRepository carts = mock(CartRepository.class);
        CartItemRepository cartItems = mock(CartItemRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
        OrderItemRepository orderItems = mock(OrderItemRepository.class);
        OrderStatusHistoryRepository statusHistories = mock(OrderStatusHistoryRepository.class);
        PriceHistoryRepository prices = mock(PriceHistoryRepository.class);
        BranchRepository branches = mock(BranchRepository.class);
        InventoryBalanceRepository balances = mock(InventoryBalanceRepository.class);
        StockHoldRepository holds = mock(StockHoldRepository.class);
        VoucherRepository vouchers = mock(VoucherRepository.class);
        VoucherRedemptionRepository redemptions = mock(VoucherRedemptionRepository.class);
        OrderDiscountLineRepository discountLines = mock(OrderDiscountLineRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        OnlineOrderServiceImpl service = new OnlineOrderServiceImpl(
                carts, cartItems, customers, orders, orderItems, statusHistories,
                prices, branches, balances, holds, vouchers, redemptions, discountLines, events);

        Cart cart = new Cart();
        cart.setStatus("ACTIVE");
        ProductVariant v = sampleVariant(10L, "ROG G16", "i7 / 16GB", 30_000_000L);
        CartItem ci = new CartItem();
        ci.setCart(cart);
        ci.setVariant(v);
        ci.setQuantity(1);

        Branch b = sampleBranch(1L);
        InventoryBalance bal = new InventoryBalance(v, b, 5, 0);

        PriceHistory ph = new PriceHistory();
        ph.setVariant(v);
        ph.setPrice(BigDecimal.valueOf(30_000_000L));
        ph.setStatus("APPROVED");

        Voucher voucher = new Voucher();
        voucher.setCode("LAP10");
        voucher.setStatus("ACTIVE");
        voucher.setDiscountType("PERCENT");
        voucher.setDiscountValue(BigDecimal.TEN);
        voucher.setMaxDiscount(BigDecimal.valueOf(2_000_000L));
        voucher.setMinOrder(BigDecimal.valueOf(10_000_000L));
        voucher.setStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        voucher.setEndAt(Instant.now().plus(10, ChronoUnit.DAYS));

        when(carts.findByCartToken("tok-1")).thenReturn(Optional.of(cart));
        when(cartItems.findByCart_IdOrderByIdAsc(any())).thenReturn(List.of(ci));
        when(branches.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(b));
        when(balances.findById_VariantId(10L)).thenReturn(List.of(bal));
        when(balances.addHeld(eq(10L), eq(1L), eq(1), any())).thenReturn(1);
        when(vouchers.findByCodeIgnoreCase("LAP10")).thenReturn(Optional.of(voucher));
        when(customers.findByPhone("0901234567")).thenReturn(Optional.of(new Customer()));
        when(prices.findFirstByVariant_IdAndStatusOrderByEffectiveFromDesc(10L, "APPROVED")).thenReturn(Optional.of(ph));

        String code = service.placeOrder("tok-1",
                new CheckoutRequest("An", "0901234567", null, "PICKUP", null, "LAP10"));

        assertThat(code).startsWith("LS");
        assertThat(voucher.getReservedCount()).isEqualTo(1);
        verify(holds).save(any(StockHold.class));
        verify(redemptions).save(any(VoucherRedemption.class));
        verify(cartItems).deleteByCart_Id(any());
    }

    @Test
    void counterOpenAndCompleteMarksSerialSold() {
        CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
        OrderItemRepository orderItems = mock(OrderItemRepository.class);
        OrderStatusHistoryRepository statusHistories = mock(OrderStatusHistoryRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        StockHoldRepository holds = mock(StockHoldRepository.class);
        InventoryBalanceRepository balances = mock(InventoryBalanceRepository.class);
        SerialStatusHistoryRepository serialHistories = mock(SerialStatusHistoryRepository.class);
        PriceHistoryRepository prices = mock(PriceHistoryRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        BranchRepository branches = mock(BranchRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        CounterTx tx = new CounterTx(orders, orderItems, statusHistories, customers, payments,
                holds, balances, serialHistories, prices, users, branches, events);

        Branch b = sampleBranch(1L);
        AppUser staff = new AppUser();
        staff.setUsername("sales01");
        staff.setBranch(b);

        when(users.findByUsernameAndDeletedAtIsNull("sales01")).thenReturn(Optional.of(staff));
        when(orders.findFirstByChannelAndStatusAndHandledBy_UsernameOrderByIdDesc("COUNTER", "DRAFT", "sales01"))
                .thenReturn(Optional.empty());

        CounterOrderView opened = tx.open("sales01");
        assertThat(opened.code()).startsWith("CT");

        ProductVariant v = sampleVariant(2L, "MacBook Air", "M3 / 8GB", 27_990_000L);
        SerialItem sn = new SerialItem();
        sn.setSerialNumber("MBA-001");
        sn.setStatus("IN_STOCK");
        sn.setVariant(v);
        sn.setBranch(b);

        CustomerOrder draft = new CustomerOrder();
        draft.setOrderCode(opened.code());
        draft.setChannel("COUNTER");
        draft.setStatus("DRAFT");

        StockHold hold = new StockHold();
        hold.setOrder(draft);
        hold.setHoldType("SERIAL");
        hold.setStatus("ACTIVE");
        hold.setVariant(v);
        hold.setBranch(b);
        hold.setSerial(sn);
        hold.setQuantity(1);
        hold.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        PriceHistory ph = new PriceHistory();
        ph.setVariant(v);
        ph.setPrice(BigDecimal.valueOf(27_990_000L));

        when(orders.findById(100L)).thenReturn(Optional.of(draft));
        when(holds.findByOrder_IdAndStatusOrderByIdAsc(100L, "ACTIVE")).thenReturn(List.of(hold));
        when(prices.findFirstByVariant_IdAndStatusOrderByEffectiveFromDesc(2L, "APPROVED")).thenReturn(Optional.of(ph));
        when(balances.addHeld(eq(2L), eq(1L), eq(-1), any())).thenReturn(1);
        when(balances.addOnHand(eq(2L), eq(1L), eq(-1), any())).thenReturn(1);

        String completedCode = tx.complete(100L, "Bình", "0912345678", "sales01");
        assertThat(completedCode).isEqualTo(opened.code());
        assertThat(draft.getStatus()).isEqualTo("COMPLETED");
        assertThat(sn.getStatus()).isEqualTo("SOLD");
        assertThat(hold.getStatus()).isEqualTo("CONVERTED");
    }

    @Test
    void orderProcessingCancelReleasesHoldsAndPdfGenerates() {
        CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
        OrderItemRepository orderItems = mock(OrderItemRepository.class);
        OrderStatusHistoryRepository statusHistories = mock(OrderStatusHistoryRepository.class);
        StockHoldRepository holds = mock(StockHoldRepository.class);
        SerialItemRepository serials = mock(SerialItemRepository.class);
        SerialStatusHistoryRepository serialHistories = mock(SerialStatusHistoryRepository.class);
        InventoryBalanceRepository balances = mock(InventoryBalanceRepository.class);
        VoucherRedemptionRepository redemptions = mock(VoucherRedemptionRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        OrderTx tx = new OrderTx(orders, orderItems, statusHistories, holds, serials, serialHistories,
                balances, redemptions, payments, users, events);

        AppUser staff = new AppUser();
        staff.setUsername("sales01");
        ProductVariant v = sampleVariant(3L, "Dell XPS", "U7 / 16GB", 32_500_000L);
        Branch b = sampleBranch(1L);

        CustomerOrder order = new CustomerOrder();
        order.setOrderCode("LS2611-0001");
        order.setStatus("NEW");
        order.setCustomerName("An");
        order.setCustomerPhone("0901234567");
        order.setSubtotal(BigDecimal.valueOf(32_500_000L));
        order.setDiscountTotal(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.valueOf(32_500_000L));
        order.setPlacedAt(Instant.now());

        StockHold qtyHold = new StockHold();
        qtyHold.setOrder(order);
        qtyHold.setVariant(v);
        qtyHold.setBranch(b);
        qtyHold.setHoldType("QUANTITY");
        qtyHold.setQuantity(1);
        qtyHold.setStatus("ACTIVE");

        when(orders.findById(50L)).thenReturn(Optional.of(order));
        when(users.findByUsernameAndDeletedAtIsNull("sales01")).thenReturn(Optional.of(staff));
        when(holds.findByOrder_IdAndStatusOrderByIdAsc(50L, "ACTIVE")).thenReturn(List.of(qtyHold));
        when(redemptions.findByOrder_IdAndStatus(50L, "RESERVED")).thenReturn(List.of());

        tx.cancel(50L, "Khách đổi ý", "sales01");
        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        assertThat(qtyHold.getStatus()).isEqualTo("RELEASED");
        verify(balances).addHeld(eq(3L), eq(1L), eq(-1), any());

        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setVariant(v);
        oi.setQuantity(1);
        oi.setUnitPrice(BigDecimal.valueOf(32_500_000L));
        oi.setLineTotal(BigDecimal.valueOf(32_500_000L));
        when(orderItems.findByOrder_IdOrderByIdAsc(50L)).thenReturn(List.of(oi));

        OrderQueryServiceImpl query = new OrderQueryServiceImpl(orders, orderItems, serials, holds);
        byte[] pdf = query.invoicePdf(50L);
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    private static ProductVariant sampleVariant(Long id, String productName, String variantName, long price) {
        Product p = new Product();
        p.setName(productName);
        p.setStatus("ACTIVE");
        ProductVariant v = mock(ProductVariant.class);
        when(v.getId()).thenReturn(id);
        when(v.getProduct()).thenReturn(p);
        when(v.getVariantName()).thenReturn(variantName);
        when(v.getCurrentPrice()).thenReturn(BigDecimal.valueOf(price));
        when(v.getWarrantyMonths()).thenReturn(12);
        when(v.getStatus()).thenReturn("ACTIVE");
        return v;
    }

    private static Branch sampleBranch(Long id) {
        Branch b = mock(Branch.class);
        when(b.getId()).thenReturn(id);
        when(b.getName()).thenReturn("Lapstore Hà Đông");
        return b;
    }
}
