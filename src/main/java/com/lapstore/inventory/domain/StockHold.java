package com.lapstore.inventory.domain;

import java.time.Instant;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.staff.domain.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "stock_hold")
public class StockHold extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "hold_type", nullable = false, length = 10)
    private String holdType;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "serial_id")
    private SerialItem serial;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "held_by")
    private AppUser heldBy;

    @Column(name = "held_at", nullable = false)
    private Instant heldAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    public Long getId() { return id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getHoldType() { return holdType; }
    public void setHoldType(String holdType) { this.holdType = holdType; }

    public SerialItem getSerial() { return serial; }
    public void setSerial(SerialItem serial) { this.serial = serial; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AppUser getHeldBy() { return heldBy; }
    public void setHeldBy(AppUser heldBy) { this.heldBy = heldBy; }

    public Instant getHeldAt() { return heldAt; }
    public void setHeldAt(Instant heldAt) { this.heldAt = heldAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }
}
