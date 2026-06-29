package com.vfa.vault.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseDateAmountProjection {

    LocalDate getExpenseDate();

    BigDecimal getTotal();
}
