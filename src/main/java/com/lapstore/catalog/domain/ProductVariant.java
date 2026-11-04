package com.lapstore.catalog.domain;

import java.math.BigDecimal;

import com.lapstore.common.persistence.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "product_variant")
public class ProductVariant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "sku", nullable = false, length = 60)
    private String sku;

    @Column(name = "variant_name", nullable = false, length = 255)
    private String variantName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(name = "cpu", nullable = false, length = 100)
    private String cpu;

    @Column(name = "gpu", length = 100)
    private String gpu;

    @Column(name = "ram_gb", nullable = false)
    private Integer ramGb;

    @Column(name = "storage_gb", nullable = false)
    private Integer storageGb;

    @Column(name = "current_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "warranty_months", nullable = false)
    private Integer warrantyMonths = 12;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 3;

    @Column(name = "return_window_days")
    private Integer returnWindowDays;

    @Column(name = "status", nullable = false, length = 15)
    private String status;

    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public String getCpu() { return cpu; }
    public void setCpu(String cpu) { this.cpu = cpu; }

    public String getGpu() { return gpu; }
    public void setGpu(String gpu) { this.gpu = gpu; }

    public Integer getRamGb() { return ramGb; }
    public void setRamGb(Integer ramGb) { this.ramGb = ramGb; }

    public Integer getStorageGb() { return storageGb; }
    public void setStorageGb(Integer storageGb) { this.storageGb = storageGb; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public Integer getReturnWindowDays() { return returnWindowDays; }
    public void setReturnWindowDays(Integer returnWindowDays) { this.returnWindowDays = returnWindowDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
