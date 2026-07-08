package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.PetProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {
    Optional<PetProfile> findByPetId(String petId);
    boolean existsByPetId(String petId);
    List<PetProfile> findByEnabledTrueOrderByUpdatedAtDesc();

    /** 按用户查询启用的档案；多用户隔离后列表查询使用。 */
    List<PetProfile> findByUserIdAndEnabledTrueOrderByUpdatedAtDesc(Long userId);

    /** 按 petId + 用户查询；档案不存在或不属于该用户时返回空，天然实现越权隐藏。 */
    Optional<PetProfile> findByPetIdAndUserId(String petId, Long userId);

    /** 判断 petId 是否属于指定用户；子资源（体重/病历/记账/照片）做归属校验用。 */
    boolean existsByPetIdAndUserId(String petId, Long userId);
}
