package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PetPhotoCreateRequest {
    @Size(max = 32) private String mediaType;
    @Size(max = 128) private String title;
    @Size(max = 512) private String fileUrl;        // 可空：截图走 imageBase64，不再强制 URL
    @Size(max = 512) private String thumbnailUrl;
    @Size(max = 255) private String tags;
    private String imageBase64;                     // 截图 base64（JPEG），存 image_data LONGTEXT
    @NotNull @PastOrPresent private LocalDateTime capturedAt;
}
