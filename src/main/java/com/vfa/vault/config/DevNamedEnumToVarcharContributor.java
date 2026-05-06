package com.vfa.vault.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

public class DevNamedEnumToVarcharContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        typeContributions.getTypeConfiguration()
            .getJdbcTypeRegistry()
            .addDescriptor(SqlTypes.NAMED_ENUM, VarcharJdbcType.INSTANCE);
    }
}
