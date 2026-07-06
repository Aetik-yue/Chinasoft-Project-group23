package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.dto.PetLedgerRecordRequest;
import com.chinasoft.smokesensor.entity.PetLedgerRecord;
import com.chinasoft.smokesensor.repository.PetLedgerRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetLedgerRecordServiceImplTest {
    @Mock PetProfileRepository profileRepository;
    @Mock PetLedgerRecordRepository recordRepository;
    @InjectMocks PetLedgerRecordServiceImpl service;

    @Test
    void createUsesDefaultUserAndCurrency() {
        when(profileRepository.existsByPetId("PET-1")).thenReturn(true);
        when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.createRecord("PET-1", request("36"));
        assertThat(response.getLedgerId()).startsWith("LED-");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void updateKeepsLedgerIdAndRejectsInvalidAmount() {
        when(profileRepository.existsByPetId("PET-1")).thenReturn(true);
        PetLedgerRecord record = PetLedgerRecord.builder().ledgerId("LED-1").petId("PET-1").build();
        when(recordRepository.findByLedgerIdAndPetId("LED-1", "PET-1")).thenReturn(Optional.of(record));
        when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.updateRecord("PET-1", "LED-1", request("88")).getLedgerId()).isEqualTo("LED-1");
        assertThatThrownBy(() -> service.createRecord("PET-1", request("0"))).isInstanceOf(IllegalArgumentException.class);
    }

    private PetLedgerRecordRequest request(String amount) {
        PetLedgerRecordRequest request = new PetLedgerRecordRequest();
        request.setExpenseDate(LocalDate.now());
        request.setDescription("主粮");
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
