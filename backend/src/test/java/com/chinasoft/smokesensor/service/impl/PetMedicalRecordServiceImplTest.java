package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetMedicalRecordRequest;
import com.chinasoft.smokesensor.entity.PetMedicalRecord;
import com.chinasoft.smokesensor.repository.PetMedicalRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetMedicalRecordServiceImplTest {
    @Mock PetProfileRepository profileRepository;
    @Mock PetMedicalRecordRepository recordRepository;
    @InjectMocks PetMedicalRecordServiceImpl service;

    @BeforeEach
    void setCurrentUser() {
        UserContext.setCurrentUserId(1L);
    }

    @AfterEach
    void clearCurrentUser() {
        UserContext.clear();
    }

    @Test
    void createGeneratesBusinessId() {
        when(profileRepository.existsByPetIdAndUserId("PET-1", 1L)).thenReturn(true);
        when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.createRecord("PET-1", request("观察到食量下降"));
        assertThat(response.getRecordId()).startsWith("MED-");
    }

    @Test
    void updateKeepsBusinessIdAndChecksOwnership() {
        when(profileRepository.existsByPetIdAndUserId("PET-1", 1L)).thenReturn(true);
        PetMedicalRecord record = PetMedicalRecord.builder().recordId("MED-1").petId("PET-1").build();
        when(recordRepository.findByRecordIdAndPetId("MED-1", "PET-1")).thenReturn(Optional.of(record));
        when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.updateRecord("PET-1", "MED-1", request("已经恢复")).getRecordId()).isEqualTo("MED-1");

        when(recordRepository.findByRecordIdAndPetId("MED-X", "PET-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateRecord("PET-1", "MED-X", request("内容")))
                .isInstanceOf(BusinessException.class);
    }

    private PetMedicalRecordRequest request(String content) {
        PetMedicalRecordRequest request = new PetMedicalRecordRequest();
        request.setRecordDate(LocalDate.now());
        request.setContent(content);
        return request;
    }
}
