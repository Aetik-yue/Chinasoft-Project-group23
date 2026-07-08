package com.chinasoft.smokesensor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 宠物媒体元数据实体，对应 pet_media_record 表。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pet_media_record")
public class PetMediaRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "media_id", nullable = false, unique = true, length = 64)
    private String mediaId;
    @Column(name = "pet_id", nullable = false, length = 64)
    private String petId;
    @Column(name = "cage_id", length = 64)
    private String cageId;
    @Column(name = "media_type", nullable = false, length = 32)
    private String mediaType;
    @Column(length = 128)
    private String title;
    @Column(name = "file_url", length = 512)
    private String fileUrl;
    @Lob
    @Column(name = "image_data", columnDefinition = "LONGTEXT")
    private String imageData;
    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    @Column(length = 255)
    private String tags;
    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
