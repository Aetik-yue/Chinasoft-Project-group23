package com.chinasoft.smokesensor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetProfileResponse {
    private String petId;
    private Long userId;
    private String deviceId;
    private String name;
    private String species;
    private LocalDate birthday;
    private String sex;
    private BigDecimal weightGrams;
    private String featherColor;
    private Boolean sterilized;
    private String avatarUrl;
    private String currentStatus;
    private Boolean enabled;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
