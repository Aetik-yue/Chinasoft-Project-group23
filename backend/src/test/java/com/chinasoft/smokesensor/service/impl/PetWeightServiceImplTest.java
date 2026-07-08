package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetWeightRequest;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.entity.PetWeightRecord;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.PetWeightRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetWeightServiceImplTest {
    @Mock PetProfileRepository profileRepository;
    @Mock PetWeightRecordRepository weightRepository;
    @InjectMocks PetWeightServiceImpl service;

    @BeforeEach
    void setCurrentUser() {
        UserContext.setCurrentUserId(1L);
    }

    @AfterEach
    void clearCurrentUser() {
        UserContext.clear();
    }

    @Test
    void createWeightSynchronizesProfileCurrentWeight() {
        PetProfile profile = PetProfile.builder().petId("PET-1").build();
        when(profileRepository.findByPetIdAndUserId("PET-1", 1L)).thenReturn(Optional.of(profile));
        when(weightRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(weightRepository.findTopByPetIdOrderByMeasuredAtDesc("PET-1"))
                .thenReturn(Optional.of(PetWeightRecord.builder().weightGrams(new BigDecimal("81.20")).build()));
        PetWeightRequest request = request("81.20");

        service.createWeight("PET-1", request);

        assertThat(profile.getWeightGrams()).isEqualByComparingTo("81.20");
    }

    @Test
    void updateRejectsRecordOwnedByAnotherPet() {
        when(profileRepository.findByPetIdAndUserId("PET-1", 1L)).thenReturn(Optional.of(PetProfile.builder().petId("PET-1").build()));
        when(weightRepository.findByIdAndPetId(7L, "PET-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateWeight("PET-1", 7L, request("80")))
                .isInstanceOf(BusinessException.class);
    }

    private PetWeightRequest request(String weight) {
        PetWeightRequest request = new PetWeightRequest();
        request.setWeightGrams(new BigDecimal(weight));
        request.setMeasuredAt(LocalDateTime.now().minusMinutes(1));
        return request;
    }
}
