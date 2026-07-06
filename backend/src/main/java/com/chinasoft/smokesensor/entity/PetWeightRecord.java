package com.chinasoft.smokesensor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 体重历史实体，对应 pet_weight_record 表。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pet_weight_record")
public class PetWeightRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "pet_id", nullable = false, length = 64)
    private String petId;
    @Column(name = "weight_grams", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightGrams;
    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;
    @Column(nullable = false, length = 32)
    private String source;
    @Column(length = 500)
    private String remark;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
