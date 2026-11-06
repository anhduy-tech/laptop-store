package com.lapstore.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Gán "nav" cho layout/staff để tô sáng mục menu theo đường dẫn đang xem. */
@ControllerAdvice
class StaffNavAdvice {

    @ModelAttribute("nav")
    String nav(HttpServletRequest req) {
        String p = req.getRequestURI();
        if (p.startsWith("/staff/sales")) return "sales";
        if (p.startsWith("/staff/consult")) return "consult";
        if (p.startsWith("/staff/warehouse")) return "warehouse";
        if (p.startsWith("/admin")) return "admin";
        return "";
    }
}
