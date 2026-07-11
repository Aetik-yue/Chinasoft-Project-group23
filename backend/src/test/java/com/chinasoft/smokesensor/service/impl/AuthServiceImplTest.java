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
import com.chinasoft.smokesensor.dto.ChangePasswordRequest;
import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.RegisterRequest;
import com.chinasoft.smokesensor.dto.UserProfileUpdateRequest;
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
    void updateProfilePersistsTrimmedFieldsAndReturnsRealIdentity() {
        SysUser user = SysUser.builder()
                .id(7L)
                .username("bird01")
                .role("viewer")
                .build();
        when(sysUserRepository.findById(7L)).thenReturn(Optional.of(user));
        // 查询到自身不应被判定为用户名冲突。
        when(sysUserRepository.findByUsername("bird01")).thenReturn(Optional.of(user));
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .username(" bird01 ")
                .phone(" 13800138000 ")
                .email(" bird01@example.com ")
                .location(" 重庆市沙坪坝区 ")
                .build();

        var response = authService.updateProfile(7L, request);

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getUsername()).isEqualTo("bird01");
        assertThat(response.getPhone()).isEqualTo("13800138000");
        assertThat(response.getEmail()).isEqualTo("bird01@example.com");
        assertThat(response.getLocation()).isEqualTo("重庆市沙坪坝区");
        verify(sysUserRepository).save(user);
    }

    @Test
    void updateProfileRejectsUsernameOwnedByAnotherUser() {
        SysUser user = SysUser.builder().id(7L).username("bird01").build();
        SysUser other = SysUser.builder().id(8L).username("bird02").build();
        when(sysUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(sysUserRepository.findByUsername("bird02")).thenReturn(Optional.of(other));

        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .username("bird02")
                .build();

        assertThatThrownBy(() -> authService.updateProfile(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该用户名已被使用");
        verify(sysUserRepository, never()).save(any(SysUser.class));
    }

    @Test
    void updateProfileConvertsBlankOptionalFieldsToNull() {
        SysUser user = SysUser.builder().id(7L).username("bird01").build();
        when(sysUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(sysUserRepository.findByUsername("bird-new")).thenReturn(Optional.empty());
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .username("bird-new")
                .phone(" ")
                .email("")
                .location("   ")
                .build();

        var response = authService.updateProfile(7L, request);

        assertThat(response.getPhone()).isNull();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getLocation()).isNull();
    }

    @Test
    void meReturnsUserIdAndLocationFromDatabase() {
        SysUser user = SysUser.builder()
                .id(7L)
                .username("bird01")
                .role("viewer")
                .location("重庆市沙坪坝区")
                .build();
        when(sysUserRepository.findById(7L)).thenReturn(Optional.of(user));

        var response = authService.me("smoke-token-7-expiry-uuid");

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getLocation()).isEqualTo("重庆市沙坪坝区");
        assertThat(response.getToken()).isEqualTo("smoke-token-7-expiry-uuid");
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

    @Test
    void changePasswordSavesNewPassword() {
        SysUser user = SysUser.builder().id(7L).username("bird01").password("oldPass").build();
        when(sysUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass")
                .newPassword("newPass123")
                .build();

        authService.changePassword(7L, request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("newPass123");
    }

    @Test
    void loginWithPhoneWhenPhoneBound() {
        SysUser user = SysUser.builder().id(11L).username("bird01").phone("13823070420").password("pw123456").status(1).build();
        when(sysUserRepository.findByUsername("13823070420")).thenReturn(Optional.empty());
        when(sysUserRepository.findByPhone("13823070420")).thenReturn(Optional.of(user));

        LoginRequest request = LoginRequest.builder().account("13823070420").password("pw123456").build();

        var response = authService.login(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo("bird01");
        assertThat(response.getUserId()).isEqualTo(11L);
    }

    @Test
    void loginWithPhoneFailsWhenNotBound() {
        when(sysUserRepository.findByUsername("13900000000")).thenReturn(Optional.empty());
        when(sysUserRepository.findByPhone("13900000000")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder().account("13900000000").password("any").build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        SysUser user = SysUser.builder().id(7L).username("bird01").password("oldPass").build();
        when(sysUserRepository.findById(7L)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrongPass")
                .newPassword("newPass123")
                .build();

        assertThatThrownBy(() -> authService.changePassword(7L, request))
                .isInstanceOf(BusinessException.class);

        verify(sysUserRepository, never()).save(any(SysUser.class));
    }
}
