package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PetProfileCreateRequest {
    @NotBlank @Size(max = 64)
    private String name;
    @NotBlank @Size(max = 64)
    private String species;
    @PastOrPresent
    private LocalDate birthday;
    @Size(max = 16)
    private String sex;
    @DecimalMin(value = "0.01")
    private BigDecimal initialWeightGrams;
    @Size(max = 64) private String cageId;
    @Size(max = 64) private String deviceId;
    @Size(max = 64) private String featherColor;
    private Boolean sterilized;
    @Size(max = 500) private String avatarUrl;
    @Size(max = 32) private String currentStatus;
    @Size(max = 500) private String remark;
}
