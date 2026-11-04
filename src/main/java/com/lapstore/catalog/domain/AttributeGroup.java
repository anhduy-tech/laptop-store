package com.lapstore.catalog.domain;

import com.lapstore.common.persistence.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "attribute_group")
public class AttributeGroup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
