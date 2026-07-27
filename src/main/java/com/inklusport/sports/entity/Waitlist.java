package com.inklusport.sports.entity;

import com.inklusport.sports.enums.WaitlistStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "waitlist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Waitlist {

    @Id
    @Column(name = "id", columnDefinition = "char(36)") 
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    private Event event;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(nullable = false)
    private Boolean notified;

    private Integer position;

    @Enumerated(EnumType.STRING)
    private WaitlistStatus status;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (notified == null) notified = false;
        if (status == null) status = WaitlistStatus.waiting;
    }
}