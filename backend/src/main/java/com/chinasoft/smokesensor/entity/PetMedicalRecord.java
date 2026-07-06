package com.chinasoft.smokesensor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 病历实体，对应 pet_medical_record 表。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pet_medical_record")
public class PetMedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "record_id", nullable = false, unique = true, length = 64)
    private String recordId;
    @Column(name = "pet_id", nullable = false, length = 64)
    private String petId;
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;
    @Column(name = "record_type", nullable = false, length = 32)
    private String recordType;
    @Column(length = 128)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(name = "hospital_name", length = 128)
    private String hospitalName;
    @Column(name = "hospital_phone", length = 32)
    private String hospitalPhone;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> attachments;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
