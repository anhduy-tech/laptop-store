package com.lapstore.reporting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.inventory.domain.Branch;
import jakarta.persistence.*;

@Entity
@Table(name = "daily_variant_sales")
public class DailyVariantSales extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "units_sold")
    private Integer unitsSold = 0;

    @Column(name = "units_returned")
    private Integer unitsReturned = 0;

    @Column(name = "revenue", precision = 17, scale = 2)
    private BigDecimal revenue;

    @Column(name = "cost", precision = 17, scale = 2)
    private BigDecimal cost;

    @Column(name = "profit", precision = 17, scale = 2)
    private BigDecimal profit;

    public Long getId() { return id; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public Integer getUnitsSold() { return unitsSold; }
    public void setUnitsSold(Integer unitsSold) { this.unitsSold = unitsSold; }

    public Integer getUnitsReturned() { return unitsReturned; }
    public void setUnitsReturned(Integer unitsReturned) { this.unitsReturned = unitsReturned; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }
}
