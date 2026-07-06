package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.PetWeightRequest;
import com.chinasoft.smokesensor.dto.PetWeightResponse;
import java.util.List;

public interface PetWeightService {
    List<PetWeightResponse> listWeights(String petId);
    PetWeightResponse createWeight(String petId, PetWeightRequest request);
    PetWeightResponse updateWeight(String petId, Long id, PetWeightRequest request);
}
