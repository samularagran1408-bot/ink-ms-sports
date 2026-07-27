package com.inklusport.sports.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SportDisabilityId implements Serializable {

    @Column(name = "sport_id")
    private Long sportId;

    @Column(name = "disability_id")
    private Long disabilityId;
}