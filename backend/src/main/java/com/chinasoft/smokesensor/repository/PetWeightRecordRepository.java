package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.PetWeightRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetWeightRecordRepository extends JpaRepository<PetWeightRecord, Long> {
    List<PetWeightRecord> findByPetIdOrderByMeasuredAtDesc(String petId);
    Optional<PetWeightRecord> findByIdAndPetId(Long id, String petId);
    Optional<PetWeightRecord> findTopByPetIdOrderByMeasuredAtDesc(String petId);
}
