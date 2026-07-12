package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.PetPhotoCreateRequest;
import com.chinasoft.smokesensor.dto.PetPhotoDeleteResponse;
import com.chinasoft.smokesensor.dto.PetPhotoResponse;
import java.util.List;

/**
 * 宠物录音管理业务接口。
 */
public interface PetRecordingService {
    List<PetPhotoResponse> listRecordings(String petId);
    PetPhotoResponse createRecording(String petId, PetPhotoCreateRequest request);
    PetPhotoDeleteResponse deleteRecording(String petId, String mediaId);
}
