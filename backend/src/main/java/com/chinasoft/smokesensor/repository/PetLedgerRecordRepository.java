package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.PetLedgerRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetLedgerRecordRepository extends JpaRepository<PetLedgerRecord, Long> {
    List<PetLedgerRecord> findByPetIdOrderByExpenseDateDescCreatedAtDesc(String petId);
    Optional<PetLedgerRecord> findByLedgerIdAndPetId(String ledgerId, String petId);

    /** 注销账号时清理某只鹦鹉的全部记账记录。 */
    void deleteByPetId(String petId);
}
