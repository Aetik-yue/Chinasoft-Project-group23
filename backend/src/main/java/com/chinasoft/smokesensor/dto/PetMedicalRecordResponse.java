package com.chinasoft.smokesensor.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetMedicalRecordResponse {
    private String recordId;
    private String petId;
    private LocalDate recordDate;
    private String recordType;
    private String title;
    private String content;
    private String hospitalName;
    private String hospitalPhone;
    private List<String> attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
