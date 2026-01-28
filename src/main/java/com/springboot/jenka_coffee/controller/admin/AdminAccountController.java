package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.UploadService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/admin/account")
public class AdminAccountController {

    private final AccountService accountService;

    private final UploadService uploadService;

    public AdminAccountController(AccountService accountService, UploadService uploadService) {
        this.accountService = accountService;
        this.uploadService = uploadService;
    }

    /**
     * Hiển thị danh sách tài khoản
     */
    @GetMapping("/list")
    public String listAccounts(Model model) {
        List<Account> accounts = accountService.findAll();
        model.addAttribute("accounts", accounts);
        return "admin/accounts/account-index";
    }

    /**
     * Hiển thị form thêm tài khoản mới
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("item", new Account());
        return "admin/accounts/account-form";
    }

    /**
     * Hiển thị form chỉnh sửa tài khoản
     */
    @GetMapping("/edit/{username}")
    public String showEditForm(@PathVariable String username, Model model, RedirectAttributes redirectAttributes) {
        Account account = accountService.findById(username);
        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản!");
            return "redirect:/admin/account/list";
        }
        model.addAttribute("item", account);
        return "admin/accounts/account-form";
    }

    /**
     * Lưu tài khoản (thêm mới hoặc cập nhật)
     */
    @PostMapping("/save")
    public String saveAccount(@Valid @ModelAttribute("item") Account account,
                            BindingResult result,
                            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
                            RedirectAttributes redirectAttributes
                            ) {

        try {
            // Kiểm tra validation errors
            if (result.hasErrors()) {
                return "admin/accounts/account-form";
            }

            // Kiểm tra tài khoản mới
            boolean isNewAccount = (account.getUsername() == null || account.getUsername().trim().isEmpty());

            if (isNewAccount) {
                // Kiểm tra username đã tồn tại
                if (accountService.existsByUsername(account.getUsername())) {
                    result.rejectValue("username", "error.account", "Tên đăng nhập đã tồn tại!");
                    return "admin/accounts/account-form";
                }

                // Kiểm tra email đã tồn tại
                if (accountService.existsByEmail(account.getEmail())) {
                    result.rejectValue("email", "error.account", "Email đã được sử dụng!");
                    return "admin/accounts/account-form";
                }
            } else {
                // Tài khoản cũ - lấy thông tin hiện tại
                Account existingAccount = accountService.findById(account.getUsername());
                if (existingAccount == null) {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản!");
                    return "redirect:/admin/account/list";
                }

                // Kiểm tra email trùng (trừ email hiện tại)
                if (!existingAccount.getEmail().equals(account.getEmail()) &&
                    accountService.existsByEmail(account.getEmail())) {
                    result.rejectValue("email", "error.account", "Email đã được sử dụng!");
                    return "admin/accounts/account-form";
                }

                // Giữ nguyên mật khẩu cũ nếu không nhập mật khẩu mới
                if (account.getPasswordHash() == null || account.getPasswordHash().trim().isEmpty()) {
                    account.setPasswordHash(existingAccount.getPasswordHash());
                }

                // Giữ nguyên ảnh cũ nếu không upload ảnh mới
                if (photoFile == null || photoFile.isEmpty()) {
                    account.setPhoto(existingAccount.getPhoto());
                }
            }

            // Xử lý upload ảnh
            if (photoFile != null && !photoFile.isEmpty()) {
                try {
                    String fileName = uploadService.saveImage(photoFile);
                    account.setPhoto(fileName);
                } catch (Exception e) {
                    result.rejectValue("photo", "error.account", "Lỗi khi upload ảnh: " + e.getMessage());
                    return "admin/accounts/account-form";
                }
            }

            // Đặt giá trị mặc định
            if (account.getActivated() == null) {
                account.setActivated(true);
            }
            if (account.getAdmin() == null) {
                account.setAdmin(false);
            }

            // Lưu tài khoản
            accountService.save(account);

            String message = isNewAccount ? "Thêm tài khoản thành công!" : "Cập nhật tài khoản thành công!";
            redirectAttributes.addFlashAttribute("success", message);

            return "redirect:/admin/account/list";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "admin/accounts/account-form";
        }
    }

    /**
     * Xóa tài khoản
     */
    @PostMapping("/delete/{username}")
    public String deleteAccount(@PathVariable String username, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.findById(username);
            if (account == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản!");
                return "redirect:/admin/account/list";
            }

            // Không cho phép xóa tài khoản admin cuối cùng
            if (account.getAdmin() != null && account.getAdmin()) {
                List<Account> admins = accountService.getAdministrators();
                if (admins.size() <= 1) {
                    redirectAttributes.addFlashAttribute("error", "Không thể xóa tài khoản admin cuối cùng!");
                    return "redirect:/admin/account/list";
                }
            }

            accountService.delete(username);
            redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa tài khoản: " + e.getMessage());
        }

        return "redirect:/admin/account/list";
    }

    /**
     * Toggle trạng thái kích hoạt tài khoản
     */
    @PostMapping("/toggle-status/{username}")
    public String toggleAccountStatus(@PathVariable String username, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.findById(username);
            if (account == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản!");
                return "redirect:/admin/account/list";
            }

            // Toggle trạng thái
            account.setActivated(!account.getActivated());
            accountService.save(account);

            String status = account.getActivated() ? "kích hoạt" : "vô hiệu hóa";
            redirectAttributes.addFlashAttribute("success", "Đã " + status + " tài khoản thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/admin/account/list";
    }

    /**
     * API endpoint để kiểm tra username có tồn tại không (AJAX)
     */
    @GetMapping("/check-username")
    @ResponseBody
    public boolean checkUsername(@RequestParam String username) {
        return !accountService.existsByUsername(username);
    }

    /**
     * API endpoint để kiểm tra email có tồn tại không (AJAX)
     */
    @GetMapping("/check-email")
    @ResponseBody
    public boolean checkEmail(@RequestParam String email, @RequestParam(required = false) String currentUsername) {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            Account currentAccount = accountService.findById(currentUsername);
            if (currentAccount != null && currentAccount.getEmail().equals(email)) {
                return true; // Email hiện tại của user này
            }
        }
        return !accountService.existsByEmail(email);
    }
}