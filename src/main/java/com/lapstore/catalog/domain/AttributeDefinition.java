package com.lapstore.catalog.domain;

import com.lapstore.common.persistence.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "attribute_definition")
public class AttributeDefinition extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private AttributeGroup group;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "data_type", nullable = false, length = 10)
    private String dataType;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "applies_to", nullable = false, length = 10)
    private String appliesTo;

    @Column(name = "is_filterable")
    private boolean filterable;

    @Column(name = "is_comparable")
    private boolean comparable;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private boolean active = true;

    public Long getId() { return id; }

    public AttributeGroup getGroup() { return group; }
    public void setGroup(AttributeGroup group) { this.group = group; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getAppliesTo() { return appliesTo; }
    public void setAppliesTo(String appliesTo) { this.appliesTo = appliesTo; }

    public boolean isFilterable() { return filterable; }
    public void setFilterable(boolean filterable) { this.filterable = filterable; }

    public boolean isComparable() { return comparable; }
    public void setComparable(boolean comparable) { this.comparable = comparable; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
