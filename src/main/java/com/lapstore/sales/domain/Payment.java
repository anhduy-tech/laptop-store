package com.lapstore.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.staff.domain.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "payment")
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @Column(name = "method", nullable = false, length = 14)
    private String method;

    @Column(name = "payment_type", nullable = false, length = 8)
    private String paymentType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "tendered_amount", precision = 15, scale = 2)
    private BigDecimal tenderedAmount;

    @Column(name = "change_amount", precision = 15, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "reference_code", length = 60)
    private String referenceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "received_by")
    private AppUser receivedBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    public Long getId() { return id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getTenderedAmount() { return tenderedAmount; }
    public void setTenderedAmount(BigDecimal tenderedAmount) { this.tenderedAmount = tenderedAmount; }

    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public AppUser getReceivedBy() { return receivedBy; }
    public void setReceivedBy(AppUser receivedBy) { this.receivedBy = receivedBy; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
