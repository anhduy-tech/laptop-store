package com.lapstore.common.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.config.Portal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound() {
        return "error/404";
    }

    @ExceptionHandler(BusinessException.class)
    String business(BusinessException e, HttpServletRequest req, HttpServletResponse resp,
                    RedirectAttributes ra, Model model) {
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            // Không redirect với GET để tránh vòng lặp khi chính trang đó lỗi.
            resp.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            model.addAttribute("message", e.getMessage());
            return "error/5xx";
        }
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:" + backTarget(req);
    }

    /** Quay về trang chứa form (Referer, chỉ nhận cùng host) hoặc trang chủ của cổng hiện tại. */
    static String backTarget(HttpServletRequest req) {
        String ref = req.getHeader("Referer");
        if (ref != null) {
            try {
                URI u = URI.create(ref);
                if (req.getServerName().equalsIgnoreCase(u.getHost())) {
                    String path = (u.getRawPath() == null || u.getRawPath().isEmpty()) ? "/" : u.getRawPath();
                    return u.getRawQuery() == null ? path : path + "?" + u.getRawQuery();
                }
            } catch (IllegalArgumentException ignored) {
                // Referer hỏng -> dùng đường dẫn mặc định
            }
        }
        return Portal.byPort(req.getLocalPort()).map(Portal::prefix).orElse("/shop");
    }
}
