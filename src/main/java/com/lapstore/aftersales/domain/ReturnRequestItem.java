package com.lapstore.aftersales.domain;

import java.math.BigDecimal;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.sales.domain.OrderItem;
import jakarta.persistence.*;

@Entity
@Table(name = "return_request_item")
public class ReturnRequestItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id")
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serial_id")
    private SerialItem serial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "condition", nullable = false, length = 10)
    private String condition;

    @Column(name = "restock_decision", length = 10)
    private String restockDecision;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    public Long getId() { return id; }

    public ReturnRequest getReturnRequest() { return returnRequest; }
    public void setReturnRequest(ReturnRequest returnRequest) { this.returnRequest = returnRequest; }

    public SerialItem getSerial() { return serial; }
    public void setSerial(SerialItem serial) { this.serial = serial; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getRestockDecision() { return restockDecision; }
    public void setRestockDecision(String restockDecision) { this.restockDecision = restockDecision; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}
