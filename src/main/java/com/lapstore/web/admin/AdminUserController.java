package com.lapstore.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Text;
import com.lapstore.staff.dto.AppUserView;
import com.lapstore.staff.dto.UserCommand;
import com.lapstore.staff.service.StaffService;

import jakarta.validation.Valid;

/**
 * AD-01 danh sách nhân viên + thêm/sửa/mở khóa. Quyền USER_MANAGE được kiểm ở
 * StaffService.
 */
@Controller
@RequestMapping("/admin/users")
class AdminUserController {
    private final StaffService staff;

    AdminUserController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("users", staff.users());
        return "admin/users";
    }

    @GetMapping("/new")
    String createForm(Model model) {
        if (!model.containsAttribute("form"))
            model.addAttribute("form", new UserForm());
        formData(model);
        model.addAttribute("userId", null);
        return "admin/user-form";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") UserForm form, BindingResult br, RedirectAttributes ra) {
        if (Text.blankToNull(form.getPassword()) == null) {
            br.addError(FormErrors.error("form", "password", null, "Nhập mật khẩu cho nhân viên mới"));
        }
        if (br.hasErrors())
            return backToForm(form, FormErrors.of(br), null, "/admin/users/new", ra);
        try {
            staff.createUser(toCommand(form));
        } catch (BusinessException e) {
            return backToForm(form, null, e.getMessage(), "/admin/users/new", ra);
        }
        ra.addFlashAttribute("success", "Đã thêm nhân viên " + form.getUsername().trim().toLowerCase() + ".");
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
            f.setPhone(u.phone());
            f.setBranchId(u.branchId());
            f.setRoles(u.roles());
            f.setActive(u.active());
            model.addAttribute("form", f);
        }
        formData(model);
        model.addAttribute("userId", id);
        return "admin/user-form";
    }

    @PostMapping("/{id}")
    String update(@PathVariable long id, @Valid @ModelAttribute("form") UserForm form, BindingResult br,
            RedirectAttributes ra) {
        String path = "/admin/users/" + id;
        if (br.hasErrors())
            return backToForm(form, FormErrors.of(br), null, path, ra);
        try {
            staff.updateUser(id, toCommand(form));
        } catch (BusinessException e) {
            return backToForm(form, null, e.getMessage(), path, ra);
        }
        ra.addFlashAttribute("success", "Đã cập nhật nhân viên.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/unlock")
    String unlock(@PathVariable long id, RedirectAttributes ra) {
        staff.unlockUser(id);
        ra.addFlashAttribute("success", "Đã mở khóa tài khoản.");
        return "redirect:/admin/users";
    }

    private void formData(Model model) {
        model.addAttribute("roleCodes", staff.roleCodes());
        model.addAttribute("branches", staff.branchOptions());
    }

    private static String backToForm(UserForm form, java.util.Map<String, String> errors, String error,
            String path, RedirectAttributes ra) {
        form.setPassword(null); // không đưa mật khẩu đã nhập quay lại trình duyệt
        ra.addFlashAttribute("form", form);
        if (errors != null)
            ra.addFlashAttribute("errors", errors);
        if (error != null)
            ra.addFlashAttribute("error", error);
        return "redirect:" + path;
    }

    private static UserCommand toCommand(UserForm f) {
        return new UserCommand(f.getUsername() == null ? null : f.getUsername().trim(), f.getFullName().trim(),
                Text.blankToNull(f.getEmail()), Text.blankToNull(f.getPhone()), Text.blankToNull(f.getPassword()),
                f.getBranchId(), f.getRoles(), f.isActive());
    }
}
