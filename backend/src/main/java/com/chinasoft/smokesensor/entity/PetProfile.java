package com.chinasoft.smokesensor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 鹦鹉档案实体，对应 pet_profile 表。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pet_profile")
public class PetProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "pet_id", nullable = false, unique = true, length = 64)
    private String petId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "cage_id", length = 64)
    private String cageId;
    @Column(name = "device_id", length = 64)
    private String deviceId;
    @Column(nullable = false, length = 64)
    private String name;
    @Column(nullable = false, length = 64)
    private String species;
    private LocalDate birthday;
    @Column(nullable = false, length = 16)
    private String sex;
    @Column(name = "weight_grams", precision = 8, scale = 2)
    private BigDecimal weightGrams;
    @Column(name = "feather_color", length = 64)
    private String featherColor;
    private Boolean sterilized;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Column(name = "current_status", length = 32)
    private String currentStatus;
    private Boolean enabled;
    @Column(length = 500)
    private String remark;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
