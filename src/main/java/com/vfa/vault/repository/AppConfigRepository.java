package com.vfa.vault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.AppConfig;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, String> {
}