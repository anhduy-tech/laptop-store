package com.lapstore.inventory.domain;

import java.time.Instant;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.staff.domain.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "stocktake")
public class Stocktake extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "scope", length = 100)
    private String scope;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "started_by")
    private AppUser startedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "closed_by")
    private AppUser closedBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public AppUser getStartedBy() { return startedBy; }
    public void setStartedBy(AppUser startedBy) { this.startedBy = startedBy; }

    public AppUser getClosedBy() { return closedBy; }
    public void setClosedBy(AppUser closedBy) { this.closedBy = closedBy; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
