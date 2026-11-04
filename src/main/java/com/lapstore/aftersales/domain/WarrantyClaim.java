package com.lapstore.aftersales.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.sales.domain.Customer;
import com.lapstore.staff.domain.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "warranty_claim")
public class WarrantyClaim extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serial_id")
    private SerialItem serial;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Lob
    @Column(name = "issue_description", nullable = false)
    private String issueDescription;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "in_warranty")
    private boolean inWarranty;

    @Column(name = "status", nullable = false, length = 14)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "handled_by")
    private AppUser handledBy;

    @Column(name = "resolution", length = 255)
    private String resolution;

    @Column(name = "repair_cost", precision = 15, scale = 2)
    private BigDecimal repairCost;

    @Column(name = "completed_at")
    private Instant completedAt;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public SerialItem getSerial() { return serial; }
    public void setSerial(SerialItem serial) { this.serial = serial; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public boolean isInWarranty() { return inWarranty; }
    public void setInWarranty(boolean inWarranty) { this.inWarranty = inWarranty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AppUser getHandledBy() { return handledBy; }
    public void setHandledBy(AppUser handledBy) { this.handledBy = handledBy; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public BigDecimal getRepairCost() { return repairCost; }
    public void setRepairCost(BigDecimal repairCost) { this.repairCost = repairCost; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
