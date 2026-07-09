package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.PetMediaRecord;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetMediaRecordRepository extends JpaRepository<PetMediaRecord, Long> {
    List<PetMediaRecord> findByPetIdAndMediaTypeInOrderByCapturedAtDesc(String petId, Collection<String> mediaTypes);
    Optional<PetMediaRecord> findByMediaIdAndPetId(String mediaId, String petId);

    // 上限管理：按 petId + 媒体类型计数、取最旧的若干条（超限时清理）
    long countByPetIdAndMediaType(String petId, String mediaType);
    List<PetMediaRecord> findByPetIdAndMediaTypeOrderByCapturedAtAsc(String petId, String mediaType);

    /** 注销账号时清理某只鹦鹉的全部媒体记录。 */
    void deleteByPetId(String petId);
}
