package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.UserPreferencesRequest;
import com.chinasoft.smokesensor.entity.UserPreference;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
import java.util.List;
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
class UserPreferenceServiceImplTest {

    @Mock
    UserPreferenceRepository userPreferenceRepository;

    @InjectMocks
    UserPreferenceServiceImpl service;

    @BeforeEach
    void setCurrentUser() {
        UserContext.setCurrentUserId(1L);
    }

    @AfterEach
    void clearCurrentUser() {
        UserContext.clear();
    }

    @Test
    void getPreferencesReturnsDefaultsWhenTableIsEmpty() {
        when(userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(1L)).thenReturn(List.of());

        var response = service.getCurrentUserPreferences();

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getLanguage()).isEqualTo("zh");
        assertThat(response.getTheme()).isEqualTo("light");
        assertThat(response.getFontFamily()).isEqualTo("default");
        assertThat(response.getFontSize()).isEqualTo(16);
        assertThat(response.getFontColor()).isEqualTo("black");
        assertThat(response.getNotificationEnabled()).isTrue();
        assertThat(response.getPermissionEnabled()).isTrue();
        assertThat(response.getAvatarParrotId()).isNull();
    }

    @Test
    void updatePreferencesSavesAllProvidedKeys() {
        when(userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(1L)).thenReturn(List.of());
        when(userPreferenceRepository.findByUserIdAndPrefKey(eq(1L), anyString())).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferencesRequest request = UserPreferencesRequest.builder()
                .language(" zh ")
                .theme("dark")
                .fontFamily("default")
                .fontSize(18)
                .fontColor("black")
                .notificationEnabled(true)
                .permissionEnabled(false)
                .avatarParrotId(" PET-001 ")
                .build();

        var response = service.updateCurrentUserPreferences(request);

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(userPreferenceRepository, org.mockito.Mockito.times(8)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(UserPreference::getPrefKey, UserPreference::getPrefValue)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("language", "zh"),
                        org.assertj.core.groups.Tuple.tuple("theme", "dark"),
                        org.assertj.core.groups.Tuple.tuple("font_family", "default"),
                        org.assertj.core.groups.Tuple.tuple("font_size", "18"),
                        org.assertj.core.groups.Tuple.tuple("font_color", "black"),
                        org.assertj.core.groups.Tuple.tuple("notification_enabled", "true"),
                        org.assertj.core.groups.Tuple.tuple("permission_enabled", "false"),
                        org.assertj.core.groups.Tuple.tuple("avatar_parrot_id", "PET-001"));
        assertThat(response.getTheme()).isEqualTo("dark");
        assertThat(response.getFontSize()).isEqualTo(18);
        assertThat(response.getPermissionEnabled()).isFalse();
    }

    @Test
    void partialUpdateKeepsExistingValues() {
        UserPreference existingTheme = UserPreference.builder()
                .id(10L)
                .userId(1L)
                .prefKey("theme")
                .prefValue("dark")
                .prefGroup("theme")
                .build();
        when(userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(1L)).thenReturn(List.of(existingTheme));
        when(userPreferenceRepository.findByUserIdAndPrefKey(1L, "language")).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferencesRequest request = UserPreferencesRequest.builder().language("en").build();

        var response = service.updateCurrentUserPreferences(request);

        assertThat(response.getLanguage()).isEqualTo("en");
        assertThat(response.getTheme()).isEqualTo("dark");
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }

    @Test
    void existingPreferenceIsUpdatedWithoutCreatingDuplicateKey() {
        UserPreference existingLanguage = UserPreference.builder()
                .id(8L)
                .userId(1L)
                .prefKey("language")
                .prefValue("zh")
                .prefGroup("general")
                .build();
        when(userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(1L)).thenReturn(List.of(existingLanguage));
        when(userPreferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateCurrentUserPreferences(UserPreferencesRequest.builder().language("ja").build());

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(userPreferenceRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(8L);
        assertThat(captor.getValue().getPrefValue()).isEqualTo("ja");
        verify(userPreferenceRepository, never()).findByUserIdAndPrefKey(1L, "language");
    }

    @Test
    void rejectsUnsupportedLanguage() {
        when(userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateCurrentUserPreferences(
                UserPreferencesRequest.builder().language("fr").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("language");
    }

    @Test
    void rejectsUnsupportedTheme() {
        when(userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateCurrentUserPreferences(
                UserPreferencesRequest.builder().theme("purple").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("theme");
    }

    @Test
    void rejectsFontSizeOutOfRange() {
        when(userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateCurrentUserPreferences(
                UserPreferencesRequest.builder().fontSize(29).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fontSize");
    }
}
