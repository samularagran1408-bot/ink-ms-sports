package com.inklusport.sports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FutureRegistrationsCheckResponse {
    private boolean hasFutureRegistrations;
    private int count;

    @Builder.Default
    private List<String> eventNames = new ArrayList<>();
}
