package com.inklusport.sports.entity;

import com.inklusport.sports.enums.RoutineRegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "routine_registration",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_routine", columnNames = {"user_id", "routine_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineRegistration {

    @Id
    @Column(name = "id", columnDefinition = "char(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "routine_id", nullable = false, length = 36)
    private String routineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", insertable = false, updatable = false)
    private TrainingRoutine routine;

    @CreationTimestamp
    @Column(name = "registration_date", updatable = false)
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoutineRegistrationStatus status;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = RoutineRegistrationStatus.active;
        }
    }
}
