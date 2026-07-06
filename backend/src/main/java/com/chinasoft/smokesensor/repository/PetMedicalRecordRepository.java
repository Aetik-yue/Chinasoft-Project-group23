package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.PetMedicalRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetMedicalRecordRepository extends JpaRepository<PetMedicalRecord, Long> {
    List<PetMedicalRecord> findByPetIdOrderByRecordDateDescCreatedAtDesc(String petId);
    Optional<PetMedicalRecord> findByRecordIdAndPetId(String recordId, String petId);
}
