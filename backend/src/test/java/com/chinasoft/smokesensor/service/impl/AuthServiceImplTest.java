package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.dto.RegisterRequest;
import com.chinasoft.smokesensor.entity.SysUser;
import com.chinasoft.smokesensor.repository.SysUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    SysUserRepository sysUserRepository;

    @InjectMocks
    AuthServiceImpl authService;

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
}
