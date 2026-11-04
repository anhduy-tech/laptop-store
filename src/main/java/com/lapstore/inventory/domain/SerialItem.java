package com.lapstore.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.sales.domain.OrderItem;
import jakarta.persistence.*;

@Entity
@Table(name = "serial_item")
public class SerialItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "serial_number", nullable = false, length = 60)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "status", nullable = false, length = 12)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "goods_receipt_item_id")
    private GoodsReceiptItem goodsReceiptItem;

    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "sold_at")
    private Instant soldAt;

    @Column(name = "warranty_start")
    private LocalDate warrantyStart;

    @Column(name = "warranty_end")
    private LocalDate warrantyEnd;

    @Version
    private Integer version;

    public Long getId() { return id; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public GoodsReceiptItem getGoodsReceiptItem() { return goodsReceiptItem; }
    public void setGoodsReceiptItem(GoodsReceiptItem goodsReceiptItem) { this.goodsReceiptItem = goodsReceiptItem; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public Instant getSoldAt() { return soldAt; }
    public void setSoldAt(Instant soldAt) { this.soldAt = soldAt; }

    public LocalDate getWarrantyStart() { return warrantyStart; }
    public void setWarrantyStart(LocalDate warrantyStart) { this.warrantyStart = warrantyStart; }

    public LocalDate getWarrantyEnd() { return warrantyEnd; }
    public void setWarrantyEnd(LocalDate warrantyEnd) { this.warrantyEnd = warrantyEnd; }

    public Integer getVersion() { return version; }
}
