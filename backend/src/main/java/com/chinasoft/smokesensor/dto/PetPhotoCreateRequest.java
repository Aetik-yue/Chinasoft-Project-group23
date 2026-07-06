package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PetPhotoCreateRequest {
    @Size(max = 32) private String mediaType;
    @Size(max = 128) private String title;
    @NotBlank @Size(max = 512) private String fileUrl;
    @Size(max = 512) private String thumbnailUrl;
    @Size(max = 255) private String tags;
    @Size(max = 64) private String cageId;
    @NotNull @PastOrPresent private LocalDateTime capturedAt;
}
