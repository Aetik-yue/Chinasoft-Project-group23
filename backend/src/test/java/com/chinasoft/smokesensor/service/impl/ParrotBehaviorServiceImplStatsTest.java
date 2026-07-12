package com.chinasoft.smokesensor.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.client.QwenVisionClient;
import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.entity.ParrotBehaviorRecord;
import com.chinasoft.smokesensor.repository.ParrotBehaviorRecordRepository;
import com.chinasoft.smokesensor.service.parrot.ClipBehaviorProvider;
import com.chinasoft.smokesensor.service.parrot.ParrotDetectionProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
    @Mock private QwenVisionClient qwenVisionClient;

    @InjectMocks private ParrotBehaviorServiceImpl service;

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
        when(recordRepository.findByDeviceIdAndCheckedAtBetweenOrderByCheckedAtAsc(
                eq("SMK-001"), eq(start), eq(end))).thenReturn(records);

        Map<String, Object> result = service.getBehaviorStats("SMK-001", "week", "2026-06-10");

        assertEquals("week", result.get("range"));
        assertEquals(3L, result.get("total"));
        assertEquals(3L, result.get("totalRecords"));
        assertEquals(2L, result.get("totalEvents"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stats = (List<Map<String, Object>>) result.get("stats");
        assertEquals(List.of(
                Map.of("behavior", "鸣叫", "count", 1L, "eventCount", 1L, "recordCount", 2L),
                Map.of("behavior", "进食", "count", 1L, "eventCount", 1L, "recordCount", 1L)), stats);
        verify(recordRepository).findByDeviceIdAndCheckedAtBetweenOrderByCheckedAtAsc(
                "SMK-001", start, end);
    }

    @Test
    void invalidRangeReturnsParameterError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.getBehaviorStats("SMK-001", "year", "2026-07-08"));

        assertEquals("range 仅支持 today、day、week、month", error.getMessage());
    }
}
