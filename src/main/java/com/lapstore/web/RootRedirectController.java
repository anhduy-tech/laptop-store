package com.lapstore.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.lapstore.config.Portal;

import jakarta.servlet.http.HttpServletRequest;


@Controller 
public class RootRedirectController {
    @GetMapping("/")
    String root(HttpServletRequest req) {
        return Portal.byPort(req.getLocalPort())
                .map(p -> "redirect:" + p.prefix())
                .orElse("redirect:/shop");
    }
    
}
