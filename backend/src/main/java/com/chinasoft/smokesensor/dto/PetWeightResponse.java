package com.chinasoft.smokesensor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetWeightResponse {
    private Long id;
    private String petId;
    private BigDecimal weightGrams;
    private LocalDateTime measuredAt;
    private String source;
    private String remark;
    private LocalDateTime createdAt;
}
