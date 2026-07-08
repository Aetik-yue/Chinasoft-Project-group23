package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetLedgerRecordRequest;
import com.chinasoft.smokesensor.dto.PetLedgerRecordResponse;
import com.chinasoft.smokesensor.entity.PetLedgerRecord;
import com.chinasoft.smokesensor.repository.PetLedgerRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.service.PetLedgerRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 饲养账本新增、修改和查询业务。 */
@Service
@RequiredArgsConstructor
public class PetLedgerRecordServiceImpl implements PetLedgerRecordService {
    private final PetProfileRepository profileRepository;
    private final PetLedgerRecordRepository recordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PetLedgerRecordResponse> listRecords(String petId) {
        String normalized = requireProfile(petId);
        return recordRepository.findByPetIdOrderByExpenseDateDescCreatedAtDesc(normalized).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PetLedgerRecordResponse createRecord(String petId, PetLedgerRecordRequest request) {
        String normalized = requireProfile(petId);
        validate(request);
        PetLedgerRecord record = PetLedgerRecord.builder().ledgerId(businessId("LED"))
                .userId(UserContext.requireUserId()).petId(normalized).expenseDate(request.getExpenseDate())
                .category(defaultText(request.getCategory(), "其他")).description(request.getDescription().trim())
                .amount(request.getAmount()).currency(normalizeCurrency(request.getCurrency())).build();
        return toResponse(recordRepository.save(record));
    }

    @Override
    @Transactional
    public PetLedgerRecordResponse updateRecord(String petId, String ledgerId, PetLedgerRecordRequest request) {
        String normalized = requireProfile(petId);
        validate(request);
        PetLedgerRecord record = recordRepository.findByLedgerIdAndPetId(required(ledgerId, "ledgerId 不能为空"), normalized)
                .orElseThrow(() -> BusinessException.notFound("账本记录不存在或不属于该鹦鹉: " + ledgerId));
        record.setExpenseDate(request.getExpenseDate());
        record.setCategory(defaultText(request.getCategory(), "其他"));
        record.setDescription(request.getDescription().trim());
        record.setAmount(request.getAmount());
        record.setCurrency(normalizeCurrency(request.getCurrency()));
        return toResponse(recordRepository.save(record));
    }

    private String requireProfile(String petId) {
        String normalized = required(petId, "petId 不能为空");
        if (!profileRepository.existsByPetIdAndUserId(normalized, UserContext.requireUserId())) throw BusinessException.notFound("鹦鹉档案不存在: " + normalized);
        return normalized;
    }
    private void validate(PetLedgerRecordRequest request) {
        if (request == null || request.getExpenseDate() == null || request.getExpenseDate().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("expenseDate 不能为空且不能晚于今天");
        required(request.getDescription(), "description 不能为空");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount 必须大于 0");
    }
    private String normalizeCurrency(String value) {
        String currency = defaultText(value, "CNY").toUpperCase(Locale.ROOT);
        if (currency.length() != 3) throw new IllegalArgumentException("currency 必须是 3 位币种代码");
        return currency;
    }
    private String businessId(String prefix) { return prefix + "-" + UUID.randomUUID(); }
    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
    private String defaultText(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    private PetLedgerRecordResponse toResponse(PetLedgerRecord record) {
        return PetLedgerRecordResponse.builder().ledgerId(record.getLedgerId()).userId(record.getUserId())
                .petId(record.getPetId()).expenseDate(record.getExpenseDate()).category(record.getCategory())
                .description(record.getDescription()).amount(record.getAmount()).currency(record.getCurrency())
                .createdAt(record.getCreatedAt()).updatedAt(record.getUpdatedAt()).build();
    }
}
