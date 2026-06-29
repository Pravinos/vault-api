package com.vfa.vault.repository.projection;

import java.math.BigDecimal;

public interface CategoryIdAmountProjection {

    Integer getCategoryId();

    BigDecimal getTotal();
}
