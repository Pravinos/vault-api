package com.vfa.vault.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vfa.vault.dto.GoalDTO;
import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.entity.GoalType;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.GoalRepository;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountBalanceService accountBalanceService;

    @Mock
    private InvestmentBalanceService investmentBalanceService;

    @InjectMocks
    private GoalService goalService;

    @Test
    void create_rejectsInvalidLinkedAccountIds() {
        UUID validId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID invalidId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Account account = new Account();
        account.setId(validId);
        account.setAccountType(AccountType.CHECKING);
        when(accountRepository.findAllById(Set.of(validId, invalidId))).thenReturn(List.of(account));

        GoalDTO.Request request = new GoalDTO.Request(
                "Emergency fund",
                "Rainy day",
                new BigDecimal("1000.00"),
                GoalType.SHORT_TERM,
                null,
                Set.of(validId, invalidId));

        assertThatThrownBy(() -> goalService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("One or more account IDs are invalid");
    }
}
