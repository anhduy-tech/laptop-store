package com.lapstore.inventory.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.inventory.dto.PosFilterOptions;
import com.lapstore.inventory.dto.PosSearchCriteria;
import com.lapstore.inventory.dto.PosSerialRow;
import com.lapstore.inventory.dto.PosVariantRow;
import com.lapstore.inventory.repo.PosSearchRepository;

@Service
class PosStockServiceImpl implements PosStockService {
    private final PosSearchRepository repo;

    PosStockServiceImpl(PosSearchRepository repo) { this.repo = repo; }

    @Override
    @PreAuthorize("hasAuthority('PORTAL_SALES')")
    @Transactional(readOnly = true)
    public PosFilterOptions options() { return repo.options(); }

    @Override
    @PreAuthorize("hasAuthority('PORTAL_SALES')")
    @Transactional(readOnly = true)
    public List<PosVariantRow> search(PosSearchCriteria c) { return repo.search(c); }

    @Override
    @PreAuthorize("hasAuthority('PORTAL_SALES')")
    @Transactional(readOnly = true)
    public List<PosSerialRow> serials(long variantId, Long branchId) { return repo.serials(variantId, branchId, Instant.now()); }
}
