package com.inklusport.sports.entity;

import com.inklusport.sports.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @Column(name = "id", columnDefinition = "char(36)")
    private String id;

    @Column(name = "sport_id", nullable = false)
    private Integer sportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", insertable = false, updatable = false)
    private Sport sport;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime;

    @Column(length = 255)
    private String location;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "available_capacity", nullable = false)
    private Integer availableCapacity;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (status == null) status = EventStatus.draft;
        if (availableCapacity == null) availableCapacity = maxCapacity;
    }
}