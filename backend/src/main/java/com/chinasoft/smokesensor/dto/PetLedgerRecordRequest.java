package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PetLedgerRecordRequest {
    @NotNull @PastOrPresent private LocalDate expenseDate;
    @Size(max = 64) private String category;
    @NotBlank @Size(max = 255) private String description;
    @NotNull @DecimalMin(value = "0.01") private BigDecimal amount;
    @Size(min = 3, max = 3) private String currency;
}
