package com.lapstore.catalog.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "variant_attribute_value")
public class VariantAttributeValue {

    @EmbeddedId
    private Id id;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @MapsId("attributeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id")
    private AttributeDefinition attribute;

    @Column(name = "value_text", length = 255)
    private String valueText;

    @Column(name = "value_number", precision = 12, scale = 3)
    private BigDecimal valueNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Embeddable
    public record Id(@Column(name = "variant_id") Long variantId, @Column(name = "attribute_id") Long attributeId)
            implements Serializable {
    }

    public VariantAttributeValue(ProductVariant variant, AttributeDefinition attribute, String valueText, BigDecimal valueNumber) {
        this.variant = variant;
        this.attribute = attribute;
        this.valueText = valueText;
        this.valueNumber = valueNumber;
        this.id = new Id(variant.getId(), attribute.getId());
    }

    public VariantAttributeValue() {
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

    public AttributeDefinition getAttribute() {
        return attribute;
    }

    public void setAttribute(AttributeDefinition attribute) {
        this.attribute = attribute;
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    public BigDecimal getValueNumber() {
        return valueNumber;
    }

    public void setValueNumber(BigDecimal valueNumber) {
        this.valueNumber = valueNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
