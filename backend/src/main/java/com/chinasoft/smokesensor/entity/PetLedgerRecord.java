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

/** 饲养账本实体，对应 pet_ledger_record 表。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pet_ledger_record")
public class PetLedgerRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ledger_id", nullable = false, unique = true, length = 64)
    private String ledgerId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "pet_id", nullable = false, length = 64)
    private String petId;
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
    @Column(nullable = false, length = 64)
    private String category;
    @Column(nullable = false, length = 255)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
