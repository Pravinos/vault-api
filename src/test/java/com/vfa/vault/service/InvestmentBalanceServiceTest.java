package com.vfa.vault.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;

@ExtendWith(MockitoExtension.class)
class InvestmentBalanceServiceTest {

    @Mock
    private com.vfa.vault.repository.InvestmentCheckpointRepository checkpointRepository;

    @InjectMocks
    private InvestmentBalanceService investmentBalanceService;

    @Test
    void resolveCurrentValue_usesManualBalanceForInvestments() {
        Account account = investmentAccount(new BigDecimal("1500.00"));

        BigDecimal value = investmentBalanceService.resolveCurrentValue(
                account,
                new BigDecimal("900.00"),
                Map.of());

        assertThat(value).isEqualByComparingTo("1500.00");
    }

    @Test
    void resolveCurrentValue_usesCheckpointWhenManualBalanceMissing() {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Account account = investmentAccount(null);
        account.setId(accountId);

        BigDecimal value = investmentBalanceService.resolveCurrentValue(
                account,
                new BigDecimal("900.00"),
                Map.of(accountId, new BigDecimal("1200.00")));

        assertThat(value).isEqualByComparingTo("1200.00");
    }

    @Test
    void resolveCurrentValue_returnsContributedBalanceForNonInvestmentAccounts() {
        Account account = new Account();
        account.setAccountType(AccountType.CHECKING);

        BigDecimal value = investmentBalanceService.resolveCurrentValue(
                account,
                new BigDecimal("450.00"),
                Map.of());

        assertThat(value).isEqualByComparingTo("450.00");
    }

    private Account investmentAccount(BigDecimal manualBalance) {
        Account account = new Account();
        account.setAccountType(AccountType.INVESTMENT);
        account.setManualBalance(manualBalance);
        return account;
    }
}
