package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class PetMedicalRecordRequest {
    @NotNull @PastOrPresent
    private LocalDate recordDate;
    @Size(max = 32) private String recordType;
    @Size(max = 128) private String title;
    @NotBlank private String content;
    @Size(max = 128) private String hospitalName;
    @Size(max = 32) private String hospitalPhone;
    private List<@NotBlank @Size(max = 512) String> attachments;
}
