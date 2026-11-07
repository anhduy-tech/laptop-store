package com.lapstore.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.common.web.FormErrors;
import com.lapstore.staff.dto.ShiftAssignCommand;
import com.lapstore.staff.service.StaffService;

import jakarta.validation.Valid;

/** AD-02 xếp ca. */
@Controller
@RequestMapping("/admin/shifts")
class AdminShiftController {
    private final StaffService staff;

    AdminShiftController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping
    String view(Model model) {
        model.addAttribute("users", staff.userOptions());
        model.addAttribute("templates", staff.shiftTemplates());
        model.addAttribute("branches", staff.branchOptions());
        model.addAttribute("assignments", staff.shifts());
        return "admin/shifts";
    }

    @PostMapping
    String assign(@Valid @ModelAttribute("form") ShiftForm form, BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", FormErrors.first(br));
            return "redirect:/admin/shifts";
        }
        staff.assignShift(new ShiftAssignCommand(form.getUserId(), form.getTemplateId(),
                form.getBranchId(), form.getWorkDate()));
        ra.addFlashAttribute("success", "Đã phân ca.");
        return "redirect:/admin/shifts";
    }
}
