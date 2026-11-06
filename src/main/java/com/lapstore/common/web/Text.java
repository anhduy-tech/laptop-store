package com.lapstore.common.web;

import java.util.Arrays;
import java.util.List;

/** Chuẩn hóa chuỗi nhập từ form. */
public final class Text {
    private Text() {}

    public static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public static String orEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** Tách theo dòng, bỏ dòng trống và trùng, giữ thứ tự. */
    public static List<String> lines(String s) {
        if (s == null) return List.of();
        return Arrays.stream(s.split("\\R")).map(String::trim).filter(x -> !x.isEmpty()).distinct().toList();
    }
}
