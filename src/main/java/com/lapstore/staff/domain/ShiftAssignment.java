package com.lapstore.staff.domain;

import java.time.LocalDate;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.inventory.domain.Branch;
import jakarta.persistence.*;

@Entity
@Table(name = "shift_assignment")
public class ShiftAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id")
    private AppUser staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_template_id")
    private ShiftTemplate shiftTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "assigned_by")
    private AppUser assignedBy;

    public Long getId() { return id; }

    public AppUser getStaff() { return staff; }
    public void setStaff(AppUser staff) { this.staff = staff; }

    public ShiftTemplate getShiftTemplate() { return shiftTemplate; }
    public void setShiftTemplate(ShiftTemplate shiftTemplate) { this.shiftTemplate = shiftTemplate; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AppUser getAssignedBy() { return assignedBy; }
    public void setAssignedBy(AppUser assignedBy) { this.assignedBy = assignedBy; }
}
