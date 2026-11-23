package com.lapstore.web.sales;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.staff.service.StaffAttendanceService;

@Controller
@RequestMapping("/staff/attendance")
class StaffAttendanceController {
    private static final String PAGE = "redirect:/staff/attendance";
    private final StaffAttendanceService attendance;

    StaffAttendanceController(StaffAttendanceService attendance) {
        this.attendance = attendance;
    }

    @GetMapping
    String view(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("shifts", attendance.today(username));
        model.addAttribute("pendingHandovers", attendance.pendingHandovers(username));
        model.addAttribute("handoverHistory", attendance.handoverHistory(username));
        model.addAttribute("canManageCash", authentication.getAuthorities().stream()
                .anyMatch(a -> "ORDER_COUNTER_CREATE".equals(a.getAuthority())));
        return "sales/shift";
    }

    @PostMapping("/check-in")
    String checkIn(@RequestParam long assignmentId, @RequestParam BigDecimal openingCash,
            Principal principal, RedirectAttributes ra) {
        attendance.checkIn(assignmentId, openingCash, principal.getName());
        ra.addFlashAttribute("success", "Đã chấm công vào ca.");
        return PAGE;
    }

    @PostMapping("/check-out")
    String checkOut(@RequestParam long assignmentId, @RequestParam(required = false) Long receiverId,
            @RequestParam BigDecimal countedCash, @RequestParam(required = false) String note,
            Principal principal, RedirectAttributes ra) {
        attendance.checkOut(assignmentId, receiverId, countedCash, note, principal.getName());
        ra.addFlashAttribute("success", receiverId == null
                ? "Đã chấm công ra và khóa sổ tiền cuối ca."
                : "Đã gửi biên bản giao ca cho nhân viên nhận.");
        return PAGE;
    }

    @PostMapping("/handover/{id}/confirm")
    String confirm(@PathVariable long id, @RequestParam BigDecimal receivedCash,
            @RequestParam(required = false) String note, Principal principal, RedirectAttributes ra) {
        attendance.confirmHandover(id, receivedCash, note, principal.getName());
        ra.addFlashAttribute("success", "Đã xác nhận nhận bàn giao.");
        return PAGE;
    }

    @ExceptionHandler({ BusinessException.class, NotFoundException.class })
    String error(RuntimeException e, RedirectAttributes ra) {
        ra.addFlashAttribute("error", e.getMessage());
        return PAGE;
    }
}
