package com.lapstore.catalog.domain;

import com.lapstore.common.persistence.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "attribute_option")
public class AttributeOption extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id")
    private AttributeDefinition attribute;

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private boolean active = true;

    public Long getId() { return id; }

    public AttributeDefinition getAttribute() { return attribute; }
    public void setAttribute(AttributeDefinition attribute) { this.attribute = attribute; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
