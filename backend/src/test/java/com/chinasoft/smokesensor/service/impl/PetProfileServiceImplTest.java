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
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.PetWeightRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
