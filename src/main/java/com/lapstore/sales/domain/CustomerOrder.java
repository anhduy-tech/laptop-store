package com.lapstore.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.domain.ShiftAssignment;
import jakarta.persistence.*;

@Entity
@Table(name = "customer_order")
public class CustomerOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, length = 30)
    private String orderCode;

    @Column(name = "channel", nullable = false, length = 8)
    private String channel;

    @Column(name = "status", nullable = false, length = 12)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_email", length = 120)
    private String customerEmail;

    @Column(name = "fulfillment_method", nullable = false, length = 10)
    private String fulfillmentMethod;

    @Column(name = "desired_receipt_date")
    private LocalDate desiredReceiptDate;

    @Column(name = "ship_province_code", length = 20)
    private String shipProvinceCode;

    @Column(name = "ship_district_code", length = 20)
    private String shipDistrictCode;

    @Column(name = "ship_ward_code", length = 20)
    private String shipWardCode;

    @Column(name = "ship_address_line", length = 255)
    private String shipAddressLine;

    @Column(name = "note", length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "fulfill_branch_id")
    private Branch fulfillBranch;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "handled_by")
    private AppUser handledBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "confirmed_by")
    private AppUser confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "shift_assignment_id")
    private ShiftAssignment shiftAssignment;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_status", nullable = false, length = 10)
    private String paymentStatus;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Version
    private Integer version;

    public Long getId() { return id; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getFulfillmentMethod() { return fulfillmentMethod; }
    public void setFulfillmentMethod(String fulfillmentMethod) { this.fulfillmentMethod = fulfillmentMethod; }

    public LocalDate getDesiredReceiptDate() { return desiredReceiptDate; }
    public void setDesiredReceiptDate(LocalDate desiredReceiptDate) { this.desiredReceiptDate = desiredReceiptDate; }

    public String getShipProvinceCode() { return shipProvinceCode; }
    public void setShipProvinceCode(String shipProvinceCode) { this.shipProvinceCode = shipProvinceCode; }

    public String getShipDistrictCode() { return shipDistrictCode; }
    public void setShipDistrictCode(String shipDistrictCode) { this.shipDistrictCode = shipDistrictCode; }

    public String getShipWardCode() { return shipWardCode; }
    public void setShipWardCode(String shipWardCode) { this.shipWardCode = shipWardCode; }

    public String getShipAddressLine() { return shipAddressLine; }
    public void setShipAddressLine(String shipAddressLine) { this.shipAddressLine = shipAddressLine; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Branch getFulfillBranch() { return fulfillBranch; }
    public void setFulfillBranch(Branch fulfillBranch) { this.fulfillBranch = fulfillBranch; }

    public AppUser getHandledBy() { return handledBy; }
    public void setHandledBy(AppUser handledBy) { this.handledBy = handledBy; }

    public AppUser getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(AppUser confirmedBy) { this.confirmedBy = confirmedBy; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }

    public ShiftAssignment getShiftAssignment() { return shiftAssignment; }
    public void setShiftAssignment(ShiftAssignment shiftAssignment) { this.shiftAssignment = shiftAssignment; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Instant getPlacedAt() { return placedAt; }
    public void setPlacedAt(Instant placedAt) { this.placedAt = placedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public Integer getVersion() { return version; }
}
