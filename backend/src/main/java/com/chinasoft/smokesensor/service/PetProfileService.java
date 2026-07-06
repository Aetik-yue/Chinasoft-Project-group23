package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.PetProfileCreateRequest;
import com.chinasoft.smokesensor.dto.PetProfileResponse;
import com.chinasoft.smokesensor.dto.PetProfileUpdateRequest;
import java.util.List;

public interface PetProfileService {
    List<PetProfileResponse> listProfiles();
    PetProfileResponse getProfile(String petId);
    PetProfileResponse createProfile(PetProfileCreateRequest request);
    PetProfileResponse updateProfile(String petId, PetProfileUpdateRequest request);
}
