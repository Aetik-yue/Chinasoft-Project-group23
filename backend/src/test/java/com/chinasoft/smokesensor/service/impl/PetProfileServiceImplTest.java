package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetProfileCreateRequest;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.entity.PetWeightRecord;
import com.chinasoft.smokesensor.repository.PetLedgerRecordRepository;
import com.chinasoft.smokesensor.repository.ParrotBehaviorRecordRepository;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetMedicalRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.PetWeightRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetProfileServiceImplTest {
    @Mock PetProfileRepository profileRepository;
    @Mock PetWeightRecordRepository weightRepository;
    @InjectMocks PetProfileServiceImpl service;

    @BeforeEach
    void setCurrentUser() {
        UserContext.setCurrentUserId(1L);
    }

    @AfterEach
    void clearCurrentUser() {
        UserContext.clear();
    }

    @Mock PetMedicalRecordRepository medicalRepository;
    @Mock PetLedgerRecordRepository ledgerRepository;
    @Mock PetMediaRecordRepository photoRepository;
    @Mock ParrotBehaviorRecordRepository behaviorRecordRepository;

    @Test
    void deleteProfileCascadesThroughSubResources() {
        PetProfile profile = PetProfile.builder().petId("PET-1").userId(1L).build();
        when(profileRepository.findByPetIdAndUserId("PET-1", 1L)).thenReturn(Optional.of(profile));

        service.deleteProfile("PET-1");

        verify(weightRepository).deleteByPetId("PET-1");
        verify(medicalRepository).deleteByPetId("PET-1");
        verify(ledgerRepository).deleteByPetId("PET-1");
        verify(photoRepository).deleteByPetId("PET-1");
        verify(behaviorRecordRepository).deleteByPetId("PET-1");
        verify(profileRepository).delete(profile);
    }

    @Test
    void createProfileAlsoCreatesInitialWeight() {
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PetProfileCreateRequest request = new PetProfileCreateRequest();
        request.setName("小太阳");
        request.setSpecies("太阳锥尾鹦鹉");
        request.setInitialWeightGrams(new BigDecimal("78.50"));

        var response = service.createProfile(request);

        assertThat(response.getPetId()).startsWith("PET-");
        assertThat(response.getUserId()).isEqualTo(1L);
        ArgumentCaptor<PetWeightRecord> captor = ArgumentCaptor.forClass(PetWeightRecord.class);
        verify(weightRepository).save(captor.capture());
        assertThat(captor.getValue().getPetId()).isEqualTo(response.getPetId());
        assertThat(captor.getValue().getWeightGrams()).isEqualByComparingTo("78.50");
    }

    @Test
    void rejectsFutureBirthday() {
        PetProfileCreateRequest request = new PetProfileCreateRequest();
        request.setName("测试");
        request.setSpecies("测试");
        request.setBirthday(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.createProfile(request)).isInstanceOf(IllegalArgumentException.class);
    }
}
