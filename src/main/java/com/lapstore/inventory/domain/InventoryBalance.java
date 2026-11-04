package com.lapstore.inventory.domain;

import java.io.Serializable;
import java.time.Instant;

import com.lapstore.catalog.domain.ProductVariant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_balance")
public class InventoryBalance {

    @EmbeddedId
    private Id id;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @MapsId("branchId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "on_hand", nullable = false)
    private Integer onHand;

    @Column(name = "held_qty", nullable = false)
    private Integer heldQty;

    @Version
    private Integer version;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Embeddable
    public record Id(@Column(name = "variant_id") Long variantId, @Column(name = "branch_id") Long branchId)
            implements Serializable {
    }

    public InventoryBalance(ProductVariant variant, Branch branch, Integer onHand, Integer heldQty) {
        this.variant = variant;
        this.branch = branch;
        this.onHand = onHand;
        this.heldQty = heldQty;
        this.id = new Id(variant.getId(), branch.getId());
    }

    public InventoryBalance() {
    }


    public Id getId() {
        return id;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Integer getOnHand() {
        return onHand;
    }

    public void setOnHand(Integer onHand) {
        this.onHand = onHand;
    }

    public Integer getHeldQty() {
        return heldQty;
    }

    public void setHeldQty(Integer heldQty) {
        this.heldQty = heldQty;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
