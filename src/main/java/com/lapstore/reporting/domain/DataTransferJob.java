package com.lapstore.reporting.domain;

import java.time.Instant;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.inventory.domain.Branch;
import jakarta.persistence.*;

@Entity
@Table(name = "data_transfer_job")
public class DataTransferJob extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type", nullable = false, length = 6)
    private String jobType;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "status", nullable = false, length = 8)
    private String status;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "success_rows")
    private Integer successRows = 0;

    @Column(name = "error_rows")
    private Integer errorRows = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public Long getId() { return id; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public Integer getSuccessRows() { return successRows; }
    public void setSuccessRows(Integer successRows) { this.successRows = successRows; }

    public Integer getErrorRows() { return errorRows; }
    public void setErrorRows(Integer errorRows) { this.errorRows = errorRows; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
