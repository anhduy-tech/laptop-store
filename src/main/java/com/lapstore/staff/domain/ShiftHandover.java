package com.lapstore.staff.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.lapstore.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shift_handover")
public class ShiftHandover extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id")
    private ShiftAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private AppUser receiver;

    @Column(name = "expected_cash", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "handed_cash", nullable = false, precision = 15, scale = 2)
    private BigDecimal handedCash;

    @Column(name = "cash_variance", nullable = false, precision = 15, scale = 2)
    private BigDecimal cashVariance;

    @Column(name = "received_cash", precision = 15, scale = 2)
    private BigDecimal receivedCash;

    @Column(name = "receive_variance", precision = 15, scale = 2)
    private BigDecimal receiveVariance;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "receiver_note", length = 255)
    private String receiverNote;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private AppUser confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public Long getId() { return id; }

    public ShiftAssignment getAssignment() { return assignment; }
    public void setAssignment(ShiftAssignment assignment) { this.assignment = assignment; }

    public AppUser getReceiver() { return receiver; }
    public void setReceiver(AppUser receiver) { this.receiver = receiver; }

    public BigDecimal getExpectedCash() { return expectedCash; }
    public void setExpectedCash(BigDecimal expectedCash) { this.expectedCash = expectedCash; }

    public BigDecimal getHandedCash() { return handedCash; }
    public void setHandedCash(BigDecimal handedCash) { this.handedCash = handedCash; }

    public BigDecimal getCashVariance() { return cashVariance; }
    public void setCashVariance(BigDecimal cashVariance) { this.cashVariance = cashVariance; }

    public BigDecimal getReceivedCash() { return receivedCash; }
    public void setReceivedCash(BigDecimal receivedCash) { this.receivedCash = receivedCash; }

    public BigDecimal getReceiveVariance() { return receiveVariance; }
    public void setReceiveVariance(BigDecimal receiveVariance) { this.receiveVariance = receiveVariance; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getReceiverNote() { return receiverNote; }
    public void setReceiverNote(String receiverNote) { this.receiverNote = receiverNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AppUser getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(AppUser confirmedBy) { this.confirmedBy = confirmedBy; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}
