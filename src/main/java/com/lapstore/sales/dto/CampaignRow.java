package com.lapstore.sales.dto;

/** period đã định dạng, vd "01/11 – 30/11". */
public record CampaignRow(String name, String type, String period, String status) {}
