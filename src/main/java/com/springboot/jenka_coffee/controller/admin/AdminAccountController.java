package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.dto.request.AccountRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.service.AccountService;
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

    public AdminAccountController(AccountService accountService) {
        this.accountService = accountService;
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
        model.addAttribute("item", new AccountRequest());
        return "admin/accounts/account-form";
    }

    /**
     * Hiển thị form chỉnh sửa tài khoản
     */
    @GetMapping("/edit/{username}")
    public String showEditForm(@PathVariable String username, Model model) {
        Account account = accountService.findByIdOrThrow(username);
        model.addAttribute("item", AccountRequest.fromEntity(account));
        return "admin/accounts/account-form";
    }

    /**
     * Lưu tài khoản (thêm mới hoặc cập nhật)
     */
    @PostMapping("/save")
    public String saveAccount(
            @Valid @ModelAttribute("item") AccountRequest request,
            BindingResult result,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            RedirectAttributes redirectAttributes) {

        // Check validation errors from @Valid
        if (result.hasErrors()) {
            return "admin/accounts/account-form";
        }

        boolean isNewAccount = (request.getUsername() == null || request.getUsername().trim().isEmpty());

        // Delegate to service - exceptions handled by GlobalExceptionHandler
        if (isNewAccount) {
            accountService.createAccount(request.toEntity(), photoFile);
        } else {
            accountService.updateAccount(request.getUsername(), request.toEntity(), photoFile);
        }

        String message = isNewAccount ? "Thêm tài khoản thành công!" : "Cập nhật tài khoản thành công!";
        redirectAttributes.addFlashAttribute("success", message);

        return "redirect:/admin/account/list";
    }

    /**
     * Xóa tài khoản
     */
    @PostMapping("/delete/{username}")
    public String deleteAccount(@PathVariable String username, RedirectAttributes redirectAttributes) {
        // Service throws BusinessRuleException if cannot delete
        accountService.deleteOrThrow(username);
        redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công!");
        return "redirect:/admin/account/list";
    }

    /**
     * Toggle trạng thái kích hoạt tài khoản
     */
    @PostMapping("/toggle-status/{username}")
    public String toggleAccountStatus(@PathVariable String username, RedirectAttributes redirectAttributes) {
        Account account = accountService.toggleActivation(username);
        String status = account.getActivated() ? "kích hoạt" : "vô hiệu hóa";
        redirectAttributes.addFlashAttribute("success", "Đã " + status + " tài khoản thành công!");
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
    public boolean checkEmail(
            @RequestParam String email,
            @RequestParam(required = false) String currentUsername) {

        if (currentUsername != null && !currentUsername.isEmpty()) {
            Account currentAccount = accountService.findById(currentUsername);
            if (currentAccount != null && currentAccount.getEmail().equals(email)) {
                return true; // Email hiện tại của user này
            }
        }
        return !accountService.existsByEmail(email);
    }
}
