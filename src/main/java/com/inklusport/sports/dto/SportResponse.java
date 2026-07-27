package com.inklusport.sports.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SportResponse {
    private Integer id;
    private String name;
    private String description;
    private String difficulty;
    private String requiredMaterials;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<DisabilityResponse> disabilities;
}