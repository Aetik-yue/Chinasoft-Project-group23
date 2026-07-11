package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.PetMedicalRecordRequest;
import com.chinasoft.smokesensor.dto.PetMedicalRecordResponse;
import java.util.List;

public interface PetMedicalRecordService {
    List<PetMedicalRecordResponse> listRecords(String petId);
    PetMedicalRecordResponse createRecord(String petId, PetMedicalRecordRequest request);
    PetMedicalRecordResponse updateRecord(String petId, String recordId, PetMedicalRecordRequest request);
    String deleteRecord(String petId, String recordId);
}
