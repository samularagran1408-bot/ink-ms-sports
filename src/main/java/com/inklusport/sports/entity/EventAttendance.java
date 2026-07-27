package com.inklusport.sports.entity;

import com.inklusport.sports.enums.CheckInMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_attendance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAttendance {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "registration_id", nullable = false, length = 36)
    private String registrationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", insertable = false, updatable = false)
    private EventRegistration registration;

    @CreationTimestamp
    @Column(name = "check_in_time", updatable = false)
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_method")
    private CheckInMethod checkInMethod;

    @Column(name = "verified_by", length = 36)
    private String verifiedBy;


    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (checkInMethod == null) checkInMethod = CheckInMethod.qr;
    }
}