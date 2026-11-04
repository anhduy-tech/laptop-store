package com.lapstore.chat.domain;

import java.time.Instant;

import com.lapstore.common.persistence.AuditableEntity;
import com.lapstore.sales.domain.Customer;
import com.lapstore.staff.domain.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_session")
public class ChatSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_token", nullable = false, length = 36)
    private String sessionToken;

    @Column(name = "chat_type", nullable = false, length = 6)
    private String chatType;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "guest_name", length = 120)
    private String guestName;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "assigned_staff_id")
    private AppUser assignedStaff;

    @Column(name = "status", nullable = false, length = 8)
    private String status;

    @Column(name = "handed_over_from_ai")
    private Boolean handedOverFromAi = false;

    @Column(name = "closed_at")
    private Instant closedAt;

    public Long getId() { return id; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }

    public AppUser getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(AppUser assignedStaff) { this.assignedStaff = assignedStaff; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getHandedOverFromAi() { return handedOverFromAi; }
    public void setHandedOverFromAi(Boolean handedOverFromAi) { this.handedOverFromAi = handedOverFromAi; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
