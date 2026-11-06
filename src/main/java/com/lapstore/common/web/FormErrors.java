package com.lapstore.common.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/** BindingResult → Map<tên field, lỗi đầu tiên> để template đọc bằng errors?.get('field'). */
public final class FormErrors {
    private FormErrors() {}

    public static Map<String, String> of(BindingResult br) {
        Map<String, String> m = new LinkedHashMap<>();
        for (FieldError fe : br.getFieldErrors()) {
            m.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return m;
    }

    /** Thông báo lỗi đầu tiên (cho form dùng flash "error" thay vì lỗi theo từng ô). */
    public static String first(BindingResult br) {
        return br.getFieldErrors().stream().map(FieldError::getDefaultMessage).findFirst().orElse("Dữ liệu không hợp lệ");
    }

    public static FieldError error(String object, String field, Object value, String message) {
        return new FieldError(object, field, value, false, null, null, message);
    }
}
