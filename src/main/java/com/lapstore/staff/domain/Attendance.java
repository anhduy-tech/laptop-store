package com.lapstore.staff.domain;

import java.time.Instant;

import com.lapstore.common.persistence.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "attendance")
public class Attendance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id")
    private ShiftAssignment assignment;

    @Column(name = "check_in")
    private Instant checkIn;

    @Column(name = "check_out")
    private Instant checkOut;

    @Column(name = "late_minutes")
    private Integer lateMinutes = 0;

    @Column(name = "note", length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "adjusted_by")
    private AppUser adjustedBy;

    public Long getId() { return id; }

    public ShiftAssignment getAssignment() { return assignment; }
    public void setAssignment(ShiftAssignment assignment) { this.assignment = assignment; }

    public Instant getCheckIn() { return checkIn; }
    public void setCheckIn(Instant checkIn) { this.checkIn = checkIn; }

    public Instant getCheckOut() { return checkOut; }
    public void setCheckOut(Instant checkOut) { this.checkOut = checkOut; }

    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public AppUser getAdjustedBy() { return adjustedBy; }
    public void setAdjustedBy(AppUser adjustedBy) { this.adjustedBy = adjustedBy; }
}
