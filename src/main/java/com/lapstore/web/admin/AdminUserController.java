package com.lapstore.web.admin;

import jakarta.validation.Valid;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Text;
import com.lapstore.staff.dto.AppUserView;
import com.lapstore.staff.dto.UserCommand;
import com.lapstore.staff.service.StaffService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** AD-01 danh sách nhân viên + form thêm/sửa (admin/user-form). */
@Controller
@RequestMapping("/admin/users")
class AdminUserController {
    private final StaffService staff;

    AdminUserController(StaffService staff) { this.staff = staff; }

    @GetMapping
    String list(Model model) {
        model.addAttribute("users", staff.users());
        return "admin/users";
    }

    @GetMapping("/new")
    String createForm(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new UserForm());
        model.addAttribute("roleCodes", staff.roleCodes());
        model.addAttribute("userId", null);
        return "admin/user-form";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") UserForm form, BindingResult br, RedirectAttributes ra) {
        if (Text.blankToNull(form.getPassword()) == null) {
            br.addError(FormErrors.error("form", "password", null, "Nhập mật khẩu cho nhân viên mới"));
        }
        if (br.hasErrors()) return backToForm(form, br, "/admin/users/new", ra);
        try {
            staff.createUser(toCommand(form));
        } catch (BusinessException e) {
            ra.addFlashAttribute("form", form);
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/new";
        }
        ra.addFlashAttribute("success", "Đã thêm nhân viên " + form.getUsername() + ".");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}")
    String editForm(@PathVariable long id, Model model) {
        AppUserView u = staff.user(id);
        if (!model.containsAttribute("form")) {
            UserForm f = new UserForm();
            f.setUsername(u.username());
            f.setFullName(u.fullName());
            f.setEmail(u.email());
            f.setRoles(u.roles());
            f.setActive(u.active());
            model.addAttribute("form", f);
        }
        model.addAttribute("roleCodes", staff.roleCodes());
        model.addAttribute("userId", id);
        return "admin/user-form";
    }

    @PostMapping("/{id}")
    String update(@PathVariable long id, @Valid @ModelAttribute("form") UserForm form, BindingResult br,
                  RedirectAttributes ra) {
        if (br.hasErrors()) return backToForm(form, br, "/admin/users/" + id, ra);
        try {
            staff.updateUser(id, toCommand(form));
        } catch (BusinessException e) {
            ra.addFlashAttribute("form", form);
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/" + id;
        }
        ra.addFlashAttribute("success", "Đã cập nhật nhân viên.");
        return "redirect:/admin/users";
    }

    private static String backToForm(UserForm form, BindingResult br, String path, RedirectAttributes ra) {
        ra.addFlashAttribute("form", form);
        ra.addFlashAttribute("errors", FormErrors.of(br));
        return "redirect:" + path;
    }

    private static UserCommand toCommand(UserForm f) {
        return new UserCommand(f.getUsername().trim(), f.getFullName().trim(), Text.blankToNull(f.getEmail()),
                Text.blankToNull(f.getPassword()), f.getRoles(), f.isActive());
    }
}
