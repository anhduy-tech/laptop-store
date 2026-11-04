package com.lapstore.reporting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.inventory.domain.Branch;
import jakarta.persistence.*;

@Entity
@Table(name = "daily_sales_summary")
public class DailySalesSummary extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "total_orders")
    private Integer totalOrders = 0;

    @Column(name = "cancelled_orders")
    private Integer cancelledOrders = 0;

    @Column(name = "returned_orders")
    private Integer returnedOrders = 0;

    @Column(name = "total_units")
    private Integer totalUnits = 0;

    @Column(name = "gross_revenue", precision = 17, scale = 2)
    private BigDecimal grossRevenue;

    @Column(name = "discount_total", precision = 17, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "refund_total", precision = 17, scale = 2)
    private BigDecimal refundTotal;

    @Column(name = "net_revenue", precision = 17, scale = 2)
    private BigDecimal netRevenue;

    @Column(name = "cost", precision = 17, scale = 2)
    private BigDecimal cost;

    @Column(name = "profit", precision = 17, scale = 2)
    private BigDecimal profit;

    @Column(name = "generated_at")
    private Instant generatedAt;

    public Long getId() { return id; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }

    public Integer getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(Integer cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public Integer getReturnedOrders() { return returnedOrders; }
    public void setReturnedOrders(Integer returnedOrders) { this.returnedOrders = returnedOrders; }

    public Integer getTotalUnits() { return totalUnits; }
    public void setTotalUnits(Integer totalUnits) { this.totalUnits = totalUnits; }

    public BigDecimal getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(BigDecimal grossRevenue) { this.grossRevenue = grossRevenue; }

    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal; }

    public BigDecimal getRefundTotal() { return refundTotal; }
    public void setRefundTotal(BigDecimal refundTotal) { this.refundTotal = refundTotal; }

    public BigDecimal getNetRevenue() { return netRevenue; }
    public void setNetRevenue(BigDecimal netRevenue) { this.netRevenue = netRevenue; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
