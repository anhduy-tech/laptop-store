package com.lapstore.sales.dto;

import java.util.List;

import com.lapstore.common.dto.IdName;

/** Lựa chọn phạm vi khi tạo campaign. */
public record ScopeOptions(List<IdName> brands, List<IdName> categories) {}
