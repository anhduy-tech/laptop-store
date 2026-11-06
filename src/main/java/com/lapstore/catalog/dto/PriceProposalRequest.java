package com.lapstore.catalog.dto;

import java.time.LocalDate;

public record PriceProposalRequest(Long variantId, Long newPrice, LocalDate effectiveFrom, String reason) {}
