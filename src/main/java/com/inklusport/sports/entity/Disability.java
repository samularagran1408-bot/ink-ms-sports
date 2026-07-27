package com.inklusport.sports.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "disability")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Disability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "is_active")
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) isActive = true;
    }
}