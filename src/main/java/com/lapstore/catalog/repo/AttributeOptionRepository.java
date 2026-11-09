package com.lapstore.catalog.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.catalog.domain.AttributeOption;

public interface AttributeOptionRepository extends JpaRepository<AttributeOption, Long> {
    List<AttributeOption> findByAttribute_Id(Long attributeId);

    @Query("select o from AttributeOption o join fetch o.attribute where o.id = :id")
    Optional<AttributeOption> findDetail(@Param("id") Long id);
}
