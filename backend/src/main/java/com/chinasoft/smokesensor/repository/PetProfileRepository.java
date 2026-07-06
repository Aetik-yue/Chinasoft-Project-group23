package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.PetProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {
    Optional<PetProfile> findByPetId(String petId);
    boolean existsByPetId(String petId);
    List<PetProfile> findByEnabledTrueOrderByUpdatedAtDesc();
}
