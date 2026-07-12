package com.chinasoft.smokesensor.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.client.QwenVisionClient;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.entity.ParrotBehaviorRecord;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.repository.ParrotBehaviorRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.service.parrot.ClipBehaviorProvider;
import com.chinasoft.smokesensor.service.parrot.ParrotDetectionProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParrotBehaviorServiceImplStatsTest {

    @Mock private ParrotProperties parrotProperties;
    @Mock private ParrotDetectionProvider parrotDetectionProvider;
    @Mock private ClipBehaviorProvider clipBehaviorProvider;
    @Mock private ParrotBehaviorRecordRepository recordRepository;
    @Mock private PetProfileRepository petProfileRepository;
    @Mock private QwenVisionClient qwenVisionClient;

    @InjectMocks private ParrotBehaviorServiceImpl service;

    @BeforeEach
    void setUpUser() {
        UserContext.setCurrentUserId(7L);
    }

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void weekStatsUsesMondayToSundayAndGroupsBehaviors() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 8, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 14, 23, 59, 59, 999_999_999);
        List<ParrotBehaviorRecord> records = List.of(
                ParrotBehaviorRecord.builder().behavior("鸣叫")
                        .checkedAt(LocalDateTime.of(2026, 6, 10, 10, 0)).build(),
                ParrotBehaviorRecord.builder().behavior("鸣叫")
                        .checkedAt(LocalDateTime.of(2026, 6, 10, 10, 0, 10)).build(),
                ParrotBehaviorRecord.builder().behavior("进食")
                        .checkedAt(LocalDateTime.of(2026, 6, 10, 10, 1)).build());
        when(petProfileRepository.findByPetIdAndUserId("PET-A", 7L))
                .thenReturn(java.util.Optional.of(PetProfile.builder()
                        .petId("PET-A").userId(7L).deviceId("SMK-001").build()));
        when(recordRepository.findByPetIdAndCheckedAtBetweenOrderByCheckedAtAsc(
                eq("PET-A"), eq(start), eq(end))).thenReturn(records);

        Map<String, Object> result = service.getBehaviorStats("PET-A", "week", "2026-06-10");

        assertEquals("week", result.get("range"));
        assertEquals(3L, result.get("total"));
        assertEquals(3L, result.get("totalRecords"));
        assertEquals(2L, result.get("totalEvents"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stats = (List<Map<String, Object>>) result.get("stats");
        assertEquals(List.of(
                Map.of("behavior", "鸣叫", "count", 1L, "eventCount", 1L, "recordCount", 2L),
                Map.of("behavior", "进食", "count", 1L, "eventCount", 1L, "recordCount", 1L)), stats);
        verify(recordRepository).findByPetIdAndCheckedAtBetweenOrderByCheckedAtAsc(
                "PET-A", start, end);
    }

    @Test
    void sharedDeviceStatsRemainSeparatedByPetId() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 8, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 8, 23, 59, 59, 999_999_999);
        when(petProfileRepository.findByPetIdAndUserId("PET-A", 7L))
                .thenReturn(java.util.Optional.of(PetProfile.builder()
                        .petId("PET-A").userId(7L).deviceId("device-001").build()));
        when(petProfileRepository.findByPetIdAndUserId("PET-B", 7L))
                .thenReturn(java.util.Optional.of(PetProfile.builder()
                        .petId("PET-B").userId(7L).deviceId("device-001").build()));
        when(recordRepository.findByPetIdAndCheckedAtBetweenOrderByCheckedAtAsc("PET-A", start, end))
                .thenReturn(List.of(ParrotBehaviorRecord.builder()
                        .petId("PET-A").behavior("鸣叫").checkedAt(start.plusHours(8)).build()));
        when(recordRepository.findByPetIdAndCheckedAtBetweenOrderByCheckedAtAsc("PET-B", start, end))
                .thenReturn(List.of(
                        ParrotBehaviorRecord.builder().petId("PET-B").behavior("进食")
                                .checkedAt(start.plusHours(9)).build(),
                        ParrotBehaviorRecord.builder().petId("PET-B").behavior("排泄")
                                .checkedAt(start.plusHours(10)).build()));

        Map<String, Object> firstStats = service.getBehaviorStats("PET-A", "day", "2026-07-08");
        Map<String, Object> secondStats = service.getBehaviorStats("PET-B", "day", "2026-07-08");

        assertEquals(1L, firstStats.get("totalRecords"));
        assertEquals(2L, secondStats.get("totalRecords"));
        verify(recordRepository).findByPetIdAndCheckedAtBetweenOrderByCheckedAtAsc("PET-A", start, end);
        verify(recordRepository).findByPetIdAndCheckedAtBetweenOrderByCheckedAtAsc("PET-B", start, end);
    }

    @Test
    void invalidRangeReturnsParameterError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.getBehaviorStats("PET-A", "year", "2026-07-08"));

        assertEquals("range 仅支持 today、day、week、month", error.getMessage());
    }
}
