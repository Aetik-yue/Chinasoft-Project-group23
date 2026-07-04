package com.chinasoft.smokesensor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "device_control")
public class DeviceControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "control_type", nullable = false, length = 64)
    private String controlType;

    @Column(name = "control_name", length = 128)
    private String controlName;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "auto_linkage", nullable = false)
    private Boolean autoLinkage;

    @Column(name = "last_operated_at")
    private LocalDateTime lastOperatedAt;

    @Column(name = "last_operated_by", length = 64)
    private String lastOperatedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
