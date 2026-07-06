package com.chinasoft.smokesensor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetLedgerRecordResponse {
    private String ledgerId;
    private Long userId;
    private String petId;
    private LocalDate expenseDate;
    private String category;
    private String description;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
