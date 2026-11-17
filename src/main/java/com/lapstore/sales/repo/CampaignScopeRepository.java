package com.lapstore.sales.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.CampaignScope;

public interface CampaignScopeRepository extends JpaRepository<CampaignScope, Long> {

    List<CampaignScope> findByOwnerTypeAndOwnerIdIn(String ownerType, Collection<Long> ownerIds);
}
