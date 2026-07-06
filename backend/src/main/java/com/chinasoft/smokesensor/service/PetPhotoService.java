package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.PetPhotoCreateRequest;
import com.chinasoft.smokesensor.dto.PetPhotoDeleteResponse;
import com.chinasoft.smokesensor.dto.PetPhotoResponse;
import java.util.List;

public interface PetPhotoService {
    List<PetPhotoResponse> listPhotos(String petId);
    PetPhotoResponse createPhoto(String petId, PetPhotoCreateRequest request);
    PetPhotoDeleteResponse deletePhoto(String petId, String mediaId);
}
