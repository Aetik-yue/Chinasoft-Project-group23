package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetPhotoDeleteResponse {
    private String mediaId;
    private String petId;
    private LocalDateTime deletedAt;
}
