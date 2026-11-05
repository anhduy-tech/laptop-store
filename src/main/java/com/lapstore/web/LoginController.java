package com.lapstore.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** AU-01 – trang đăng nhập dùng chung cho cổng nhân viên và admin. */
@Controller
public class LoginController {

    @GetMapping("/staff/login")
    String staffLogin(Model m) {
        m.addAttribute("loginUrl", "/staff/login");
        return "login";
    }

    @GetMapping("/admin/login")
    String adminLogin(Model m) {
        m.addAttribute("loginUrl", "/admin/login");
        return "login";
    }
}
