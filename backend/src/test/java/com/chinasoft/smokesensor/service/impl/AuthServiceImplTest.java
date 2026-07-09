package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.RegisterRequest;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.entity.SysUser;
import com.chinasoft.smokesensor.repository.PetLedgerRecordRepository;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetMedicalRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.PetWeightRecordRepository;
import com.chinasoft.smokesensor.repository.SysUserRepository;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock SysUserRepository sysUserRepository;
    @Mock UserPreferenceRepository userPreferenceRepository;
    @Mock PetProfileRepository petProfileRepository;
    @Mock PetWeightRecordRepository petWeightRecordRepository;
    @Mock PetMedicalRecordRepository petMedicalRecordRepository;
    @Mock PetLedgerRecordRepository petLedgerRecordRepository;
    @Mock PetMediaRecordRepository petMediaRecordRepository;

    @InjectMocks AuthServiceImpl authService;

    @Test
    void registerWithoutPhoneSavesNullPhone() {
        when(sysUserRepository.findByUsername("bird01")).thenReturn(Optional.empty());
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = RegisterRequest.builder()
                .account("bird01")
                .password("123456")
                .phone(null)
                .build();

        var response = authService.register(request);

        assertThat(response.getToken()).isNotBlank();
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPhone()).isNull();
        assertThat(captor.getAllValues().get(0).getUsername()).isEqualTo("bird01");
    }

    @Test
    void registerWithPhoneSavesTrimmedPhone() {
        when(sysUserRepository.findByUsername("bird02")).thenReturn(Optional.empty());
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = RegisterRequest.builder()
                .account("bird02")
                .password("123456")
                .phone(" 13800138000 ")
                .build();

        authService.register(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPhone()).isEqualTo("13800138000");
    }

    @Test
    void registerWithEmptyPhoneSavesNullPhone() {
        when(sysUserRepository.findByUsername("bird03")).thenReturn(Optional.empty());
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = RegisterRequest.builder()
                .account("bird03")
                .password("123456")
                .phone("   ")
                .build();

        authService.register(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPhone()).isNull();
    }

    @Test
    void deleteAccountCascadesThroughPetRecords() {
        SysUser user = SysUser.builder().id(7L).username("bird01").build();
        PetProfile profile = PetProfile.builder().petId("PET-1").userId(7L).build();
        when(sysUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(petProfileRepository.findByUserId(7L)).thenReturn(List.of(profile));

        authService.deleteAccount(7L);

        verify(userPreferenceRepository).deleteByUserId(7L);
        verify(petWeightRecordRepository).deleteByPetId("PET-1");
        verify(petMedicalRecordRepository).deleteByPetId("PET-1");
        verify(petLedgerRecordRepository).deleteByPetId("PET-1");
        verify(petMediaRecordRepository).deleteByPetId("PET-1");
        verify(petProfileRepository).deleteAll(List.of(profile));
        verify(sysUserRepository).delete(user);
    }

    @Test
    void deleteAccountWithNoPetsOnlyCleansPreferences() {
        SysUser user = SysUser.builder().id(9L).username("loner").build();
        when(sysUserRepository.findById(9L)).thenReturn(Optional.of(user));
        when(petProfileRepository.findByUserId(9L)).thenReturn(List.of());

        authService.deleteAccount(9L);

        verify(userPreferenceRepository).deleteByUserId(9L);
        verify(petWeightRecordRepository, never()).deleteByPetId(any());
        verify(petProfileRepository).deleteAll(List.of());
        verify(sysUserRepository).delete(user);
    }

    @Test
    void deleteAccountThrowsWhenUserNotFound() {
        when(sysUserRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.deleteAccount(999L))
                .isInstanceOf(BusinessException.class);

        verify(sysUserRepository, never()).delete(any());
    }
}
