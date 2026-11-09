package com.lapstore.catalog.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.catalog.domain.AttributeDefinition;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
    Optional<AttributeDefinition> findByCode(String code);
}
