package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetPhotoCreateRequest;
import com.chinasoft.smokesensor.entity.PetMediaRecord;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
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
class PetPhotoServiceImplTest {
    @Mock PetProfileRepository profileRepository;
    @Mock PetMediaRecordRepository mediaRepository;
    @InjectMocks PetPhotoServiceImpl service;

    @BeforeEach
    void setCurrentUser() {
        UserContext.setCurrentUserId(1L);
    }

    @AfterEach
    void clearCurrentUser() {
        UserContext.clear();
    }

    @Test
    void createAndDeletePhotoMetadata() {
        when(profileRepository.existsByPetIdAndUserId("PET-1", 1L)).thenReturn(true);
        when(mediaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var created = service.createPhoto("PET-1", request());
        assertThat(created.getMediaId()).startsWith("MEDIA-");
        assertThat(created.getMediaType()).isEqualTo("photo");

        PetMediaRecord record = PetMediaRecord.builder().mediaId(created.getMediaId()).petId("PET-1").mediaType("photo").build();
        when(mediaRepository.findByMediaIdAndPetId(created.getMediaId(), "PET-1")).thenReturn(Optional.of(record));
        service.deletePhoto("PET-1", created.getMediaId());
        verify(mediaRepository).delete(record);
    }

    @Test
    void repeatedDeleteReturnsNotFound() {
        when(profileRepository.existsByPetIdAndUserId("PET-1", 1L)).thenReturn(true);
        when(mediaRepository.findByMediaIdAndPetId("MEDIA-X", "PET-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deletePhoto("PET-1", "MEDIA-X")).isInstanceOf(BusinessException.class);
    }

    private PetPhotoCreateRequest request() {
        PetPhotoCreateRequest request = new PetPhotoCreateRequest();
        request.setFileUrl("https://example.test/parrot.jpg");
        request.setCapturedAt(LocalDateTime.now().minusMinutes(1));
        return request;
    }
}
