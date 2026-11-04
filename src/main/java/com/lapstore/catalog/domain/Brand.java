package com.lapstore.catalog.domain;

import com.lapstore.common.persistence.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "brand")
public class Brand extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "is_active")
    private boolean active = true;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
