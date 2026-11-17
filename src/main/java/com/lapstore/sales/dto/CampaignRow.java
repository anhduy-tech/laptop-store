package com.lapstore.sales.dto;

/** value/scope/period đã định dạng, vd "Giảm 8%, tối đa 2.000.000đ", "Hãng ASUS", "01/11 – 30/11". status: trạng thái hiển thị. */
public record CampaignRow(Long id, String code, String name, String type, String value, String scope, String period,
                          String status, int priority, boolean stackable, boolean canToggle) {}
