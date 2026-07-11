package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.PetLedgerRecordRequest;
import com.chinasoft.smokesensor.dto.PetLedgerRecordDeleteResponse;
import com.chinasoft.smokesensor.dto.PetLedgerRecordResponse;
import java.util.List;

public interface PetLedgerRecordService {
    List<PetLedgerRecordResponse> listRecords(String petId);
    PetLedgerRecordResponse createRecord(String petId, PetLedgerRecordRequest request);
    PetLedgerRecordResponse updateRecord(String petId, String ledgerId, PetLedgerRecordRequest request);
    PetLedgerRecordDeleteResponse deleteRecord(String petId, String ledgerId);
}
