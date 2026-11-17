package com.lapstore.inventory.dto;

import java.util.List;

import com.lapstore.common.dto.IdName;

/** Giá trị đang có để đổ vào các ô lọc. */
public record PosFilterOptions(List<IdName> brands, List<String> cpus, List<String> gpus, List<Integer> rams,
                               List<Integer> storages) {}
