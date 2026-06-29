package com.vfa.vault.repository.projection;

import java.math.BigDecimal;

public interface DayAmountProjection {

    String getDay();

    BigDecimal getTotal();
}
