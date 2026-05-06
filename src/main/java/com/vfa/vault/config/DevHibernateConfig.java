package com.vfa.vault.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("dev")
public class DevHibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer namedEnumToVarcharCustomizer() {
        return props -> props.put(
            "hibernate.type_contributors",
            (TypeContributorList) () -> List.of(new NamedEnumToVarcharContributor())
        );
    }

    static class NamedEnumToVarcharContributor implements TypeContributor {
        @Override
        public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
            typeContributions.getTypeConfiguration()
                .getJdbcTypeRegistry()
                .addDescriptor(SqlTypes.NAMED_ENUM, VarcharJdbcType.INSTANCE);
        }
    }
}
