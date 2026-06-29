package com.vfa.vault.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountIdAmountProjection {

    UUID getAccountId();

    BigDecimal getTotal();
}
