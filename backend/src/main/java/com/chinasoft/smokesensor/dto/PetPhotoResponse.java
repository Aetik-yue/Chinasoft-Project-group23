package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetPhotoResponse {
    private String mediaId;
    private String petId;
    private String mediaType;
    private String title;
    private String fileUrl;
    private String imageBase64;
    private String thumbnailUrl;
    private Integer durationSeconds;                // 录音或视频时长（秒）
    private String tags;
    private LocalDateTime capturedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
