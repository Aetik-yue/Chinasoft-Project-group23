package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.PetMediaRecord;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetMediaRecordRepository extends JpaRepository<PetMediaRecord, Long> {
    List<PetMediaRecord> findByPetIdAndMediaTypeInOrderByCapturedAtDesc(String petId, Collection<String> mediaTypes);
    Optional<PetMediaRecord> findByMediaIdAndPetId(String mediaId, String petId);
}
