package com.lapstore.inventory.domain;

import java.time.Instant;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.staff.domain.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "stocktake_item")
public class StocktakeItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stocktake_id")
    private Stocktake stocktake;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serial_id")
    private SerialItem serial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "expected")
    private boolean expected;

    @Column(name = "scanned_at")
    private Instant scannedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "scanned_by")
    private AppUser scannedBy;

    @Column(name = "result", length = 12)
    private String result;

    @Column(name = "note", length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "adjusted_by")
    private AppUser adjustedBy;

    public Long getId() { return id; }

    public Stocktake getStocktake() { return stocktake; }
    public void setStocktake(Stocktake stocktake) { this.stocktake = stocktake; }

    public SerialItem getSerial() { return serial; }
    public void setSerial(SerialItem serial) { this.serial = serial; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public boolean isExpected() { return expected; }
    public void setExpected(boolean expected) { this.expected = expected; }

    public Instant getScannedAt() { return scannedAt; }
    public void setScannedAt(Instant scannedAt) { this.scannedAt = scannedAt; }

    public AppUser getScannedBy() { return scannedBy; }
    public void setScannedBy(AppUser scannedBy) { this.scannedBy = scannedBy; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public AppUser getAdjustedBy() { return adjustedBy; }
    public void setAdjustedBy(AppUser adjustedBy) { this.adjustedBy = adjustedBy; }
}
