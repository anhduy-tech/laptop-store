package com.lapstore.catalog.dto;

import java.util.List;

/** Các giá trị đang dùng được của từng thuộc tính, để đổ vào ô chọn. */
public record SpecChoices(List<Choice> cpus, List<Choice> gpus, List<Choice> rams, List<Choice> storages) {

    /** value = giá trị lưu vào biến thể (CPU/GPU: chính chuỗi; RAM/ổ cứng: số GB); label = chữ hiển thị. */
    public record Choice(String value, String label) {
    }
}
