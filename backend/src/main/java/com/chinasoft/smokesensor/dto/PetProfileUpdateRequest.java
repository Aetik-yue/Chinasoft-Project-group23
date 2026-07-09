package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PetProfileUpdateRequest {
    @Size(max = 64) private String name;
    @Size(max = 64) private String species;
    @PastOrPresent private LocalDate birthday;
    @Size(max = 16) private String sex;
    @Size(max = 64) private String deviceId;
    @Size(max = 64) private String featherColor;
    private Boolean sterilized;
    @Size(max = 500) private String avatarUrl;
    @Size(max = 32) private String currentStatus;
    @Size(max = 500) private String remark;
    private Boolean enabled;
}
