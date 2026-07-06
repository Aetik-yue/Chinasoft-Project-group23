package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PetWeightRequest {
    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal weightGrams;
    @NotNull @PastOrPresent
    private LocalDateTime measuredAt;
    @Size(max = 32) private String source;
    @Size(max = 500) private String remark;
}
