package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.PetLedgerRecordRequest;
import com.chinasoft.smokesensor.dto.PetMedicalRecordRequest;
import com.chinasoft.smokesensor.dto.PetPhotoCreateRequest;
import com.chinasoft.smokesensor.dto.PetProfileCreateRequest;
import com.chinasoft.smokesensor.dto.PetProfileUpdateRequest;
import com.chinasoft.smokesensor.dto.PetWeightRequest;
import com.chinasoft.smokesensor.service.PetLedgerRecordService;
import com.chinasoft.smokesensor.service.PetMedicalRecordService;
import com.chinasoft.smokesensor.service.PetPhotoService;
import com.chinasoft.smokesensor.service.PetProfileService;
import com.chinasoft.smokesensor.service.PetWeightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鹦鹉照护数据接口。所有子资源都放在 petId 路径下，避免前端误操作其他鹦鹉的数据。
 */
@RestController
@RequestMapping("/api/parrots")
@RequiredArgsConstructor
public class ParrotCareController {
    private final PetProfileService profileService;
    private final PetWeightService weightService;
    private final PetMedicalRecordService medicalRecordService;
    private final PetLedgerRecordService ledgerRecordService;
    private final PetPhotoService photoService;

    @GetMapping
    public ApiResult listProfiles() {
        return ApiResult.ok(profileService.listProfiles());
    }

    @GetMapping("/{petId}")
    public ApiResult getProfile(@PathVariable String petId) {
        return ApiResult.ok(profileService.getProfile(petId));
    }

    @PostMapping
    public ApiResult createProfile(@Valid @RequestBody PetProfileCreateRequest request) {
        return ApiResult.ok(profileService.createProfile(request));
    }

    @PutMapping("/{petId}")
    public ApiResult updateProfile(@PathVariable String petId, @Valid @RequestBody PetProfileUpdateRequest request) {
        return ApiResult.ok(profileService.updateProfile(petId, request));
    }

    @GetMapping("/{petId}/weights")
    public ApiResult listWeights(@PathVariable String petId) {
        return ApiResult.ok(weightService.listWeights(petId));
    }

    @PostMapping("/{petId}/weights")
    public ApiResult createWeight(@PathVariable String petId, @Valid @RequestBody PetWeightRequest request) {
        return ApiResult.ok(weightService.createWeight(petId, request));
    }

    @PutMapping("/{petId}/weights/{id}")
    public ApiResult updateWeight(@PathVariable String petId, @PathVariable Long id,
                                  @Valid @RequestBody PetWeightRequest request) {
        return ApiResult.ok(weightService.updateWeight(petId, id, request));
    }

    @GetMapping("/{petId}/medical-records")
    public ApiResult listMedicalRecords(@PathVariable String petId) {
        return ApiResult.ok(medicalRecordService.listRecords(petId));
    }

    @PostMapping("/{petId}/medical-records")
    public ApiResult createMedicalRecord(@PathVariable String petId,
                                         @Valid @RequestBody PetMedicalRecordRequest request) {
        return ApiResult.ok(medicalRecordService.createRecord(petId, request));
    }

    @PutMapping("/{petId}/medical-records/{recordId}")
    public ApiResult updateMedicalRecord(@PathVariable String petId, @PathVariable String recordId,
                                         @Valid @RequestBody PetMedicalRecordRequest request) {
        return ApiResult.ok(medicalRecordService.updateRecord(petId, recordId, request));
    }

    @GetMapping("/{petId}/ledger-records")
    public ApiResult listLedgerRecords(@PathVariable String petId) {
        return ApiResult.ok(ledgerRecordService.listRecords(petId));
    }

    @PostMapping("/{petId}/ledger-records")
    public ApiResult createLedgerRecord(@PathVariable String petId,
                                        @Valid @RequestBody PetLedgerRecordRequest request) {
        return ApiResult.ok(ledgerRecordService.createRecord(petId, request));
    }

    @PutMapping("/{petId}/ledger-records/{ledgerId}")
    public ApiResult updateLedgerRecord(@PathVariable String petId, @PathVariable String ledgerId,
                                        @Valid @RequestBody PetLedgerRecordRequest request) {
        return ApiResult.ok(ledgerRecordService.updateRecord(petId, ledgerId, request));
    }

    @GetMapping("/{petId}/photos")
    public ApiResult listPhotos(@PathVariable String petId) {
        return ApiResult.ok(photoService.listPhotos(petId));
    }

    @PostMapping("/{petId}/photos")
    public ApiResult createPhoto(@PathVariable String petId, @Valid @RequestBody PetPhotoCreateRequest request) {
        return ApiResult.ok(photoService.createPhoto(petId, request));
    }

    @DeleteMapping("/{petId}/photos/{mediaId}")
    public ApiResult deletePhoto(@PathVariable String petId, @PathVariable String mediaId) {
        return ApiResult.ok(photoService.deletePhoto(petId, mediaId));
    }
}
