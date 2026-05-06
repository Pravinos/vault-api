package com.vfa.vault.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.IncomeDTO;
import com.vfa.vault.entity.Income;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.IncomeCategoryRepository;
import com.vfa.vault.repository.IncomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final AccountRepository accountRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional(readOnly = true)
    public List<IncomeDTO.Response> getIncome(String month, UUID accountId) {
        return incomeRepository.findByFilters(month, accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public IncomeDTO.Response createIncome(IncomeDTO.Request dto) {
        var category = incomeCategoryRepository.findById(dto.incomeCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("IncomeCategory", dto.incomeCategoryId()));
        var account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", dto.accountId()));

        var income = new Income();
        income.setAmount(dto.amount());
        income.setNote(dto.note());
        income.setIncomeCategory(category);
        income.setAccount(account);
        income.setIncomeDate(dto.incomeDate());

        return toResponse(incomeRepository.save(income));
    }

    @Transactional
    public IncomeDTO.Response updateIncome(UUID id, IncomeDTO.Request dto) {
        var income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));

        var category = incomeCategoryRepository.findById(dto.incomeCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("IncomeCategory", dto.incomeCategoryId()));
        var account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", dto.accountId()));

        income.setAmount(dto.amount());
        income.setNote(dto.note());
        income.setIncomeCategory(category);
        income.setAccount(account);
        income.setIncomeDate(dto.incomeDate());

        return toResponse(incomeRepository.save(income));
    }

    @Transactional
    public void deleteIncome(UUID id) {
                // Hard delete only. Idempotent by design: deleting a missing row is a no-op.
                incomeRepository.findById(id).ifPresent(income -> {
                        incomeRepository.delete(income);
                        incomeRepository.flush();
                });
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMonthlySummary(String month) {
        final String resolvedMonth = month != null ? month : YearMonth.now().format(MONTH_FMT);
        return incomeRepository.findByFilters(resolvedMonth, null).stream()
                .collect(Collectors.groupingBy(
                        i -> i.getIncomeCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Income::getAmount, BigDecimal::add)
                ));
    }

    private IncomeDTO.Response toResponse(Income i) {
        return new IncomeDTO.Response(
                i.getId(),
                i.getAmount(),
                i.getNote(),
                i.getIncomeCategory().getId(),
                i.getAccount().getId(),
                i.getIncomeDate(),
                i.getCreatedAt(),
                i.getIncomeCategory().getName(),
                i.getIncomeCategory().getIcon(),
                i.getAccount().getName()
        );
    }
}
