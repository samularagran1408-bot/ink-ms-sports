package com.inklusport.sports.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sport")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private DifficultyLevel difficulty;

    @Column(name = "required_materials", columnDefinition = "TEXT")
    private String requiredMaterials;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "sport", cascade = CascadeType.ALL)
    private List<SportDisability> disabilities = new ArrayList<>();

    public enum DifficultyLevel {
        bajo, medio, alto
    }

    @PrePersist
    protected void onCreate() {
        if (isActive == null) isActive = true;
        if (difficulty == null) difficulty = DifficultyLevel.medio;
    }
}