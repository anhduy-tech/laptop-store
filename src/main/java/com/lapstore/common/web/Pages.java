package com.lapstore.common.web;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Cắt trang trong bộ nhớ cho các danh sách nhỏ do service trả về nguyên List (người dùng, ca, thuộc tính...). */
public final class Pages {
    /** Số dòng mỗi trang của mọi bảng quản trị (10 đến 15 dòng). */
    public static final int SIZE = 12;

    private Pages() {
    }

    public static <T> Page<T> of(List<T> all, int page) {
        return of(all, page, SIZE);
    }

    public static <T> Page<T> of(List<T> all, int page, int size) {
        int p = Math.max(page, 0);
        int from = (int) Math.min((long) p * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(p, size), all.size());
    }
}
