package com.vfa.vault.repository.projection;

import java.math.BigDecimal;

public interface TopCategoryProjection {

    String getCategoryName();

    BigDecimal getTotal();
}
