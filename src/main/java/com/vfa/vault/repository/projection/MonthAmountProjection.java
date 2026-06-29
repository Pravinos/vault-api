package com.vfa.vault.repository.projection;

import java.math.BigDecimal;

public interface MonthAmountProjection {

    String getMonth();

    BigDecimal getTotal();
}
