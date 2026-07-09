package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.PetWeightRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetWeightRecordRepository extends JpaRepository<PetWeightRecord, Long> {
    List<PetWeightRecord> findByPetIdOrderByMeasuredAtDesc(String petId);
    Optional<PetWeightRecord> findByIdAndPetId(Long id, String petId);
    Optional<PetWeightRecord> findTopByPetIdOrderByMeasuredAtDesc(String petId);

    /** 注销账号时清理某只鹦鹉的全部体重记录。 */
    void deleteByPetId(String petId);
}
