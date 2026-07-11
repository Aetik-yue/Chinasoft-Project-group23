package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/** 账本记录删除结果。 */
@Data
@Builder
public class PetLedgerRecordDeleteResponse {
    private String ledgerId;
    private String petId;
    private LocalDateTime deletedAt;
}
