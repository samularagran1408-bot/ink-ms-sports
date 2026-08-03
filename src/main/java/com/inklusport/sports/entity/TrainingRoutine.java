package com.inklusport.sports.entity;

import com.inklusport.sports.enums.RoutineLevel;
import com.inklusport.sports.enums.RoutineStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "training_routine")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingRoutine {

    @Id
    @Column(name = "id", columnDefinition = "char(36)")
    private String id;

    @Column(name = "trainer_id", nullable = false, length = 36)
    private String trainerId;

    @Column(name = "sport_id")
    private Integer sportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", insertable = false, updatable = false)
    private Sport sport;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "disability_focus", length = 100)
    private String disabilityFocus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoutineLevel level;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "exercises_json", columnDefinition = "TEXT")
    private String exercisesJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoutineStatus status;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "available_capacity", nullable = false)
    private Integer availableCapacity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = RoutineStatus.draft;
        }
        if (level == null) {
            level = RoutineLevel.principiante;
        }
        if (durationMinutes == null) {
            durationMinutes = 35;
        }
        if (maxCapacity == null) {
            maxCapacity = 20;
        }
        if (availableCapacity == null) {
            availableCapacity = maxCapacity;
        }
    }
}
