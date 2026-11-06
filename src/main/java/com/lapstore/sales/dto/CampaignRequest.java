package com.lapstore.sales.dto;

import java.time.LocalDate;

/** type: PERCENT | AMOUNT. */
public record CampaignRequest(String name, String type, Long value, LocalDate startAt, LocalDate endAt) {}
