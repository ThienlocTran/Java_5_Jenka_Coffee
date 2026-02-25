package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.dto.request.ProfileUpdateRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class  ProfileController {

    private final ProfileService profileService;

    /**
     * Show user profile page
     */
    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        String username = getCurrentUsername(session);
        if (username == null) {
            return "redirect:/auth/login";
        }

        try {
            Account account = profileService.getProfile(username);
            
            // Check if there's a flash attribute (from validation error)
            ProfileUpdateRequest profileRequest = (ProfileUpdateRequest) model.asMap().get("profileRequest");
            if (profileRequest == null) {
                // Pre-populate form with current data
                profileRequest = new ProfileUpdateRequest();
                profileRequest.setFullname(account.getFullname());
                profileRequest.setEmail(account.getEmail());
                profileRequest.setPhone(account.getPhone());
            }
            
            model.addAttribute("account", account);
            model.addAttribute("profileRequest", profileRequest);
            return "site/profile/profile";
        } catch (Exception e) {
            log.error("Error loading profile for user: {}", username, e);
            model.addAttribute("error", "Không thể tải thông tin hồ sơ");
            return "site/profile/profile";
        }
    }

    /**
     * Update user profile
     */
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute ProfileUpdateRequest request,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        String username = getCurrentUsername(session);
        if (username == null) {
            return "redirect:/auth/login";
        }

        try {
            Account updatedAccount = profileService.updateProfile(username, request);
            
            // Update session if needed
            session.setAttribute("user", updatedAccount);
            
            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công!");
            return "redirect:/profile";
            
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("profileRequest", request);
            return "redirect:/profile";
            
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("profileRequest", request);
            return "redirect:/profile";
            
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", username, e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật hồ sơ");
            redirectAttributes.addFlashAttribute("profileRequest", request);
            return "redirect:/profile";
        }
    }

    /**
     * Update user avatar
     */
    @PostMapping("/avatar")
    public String updateAvatar(@RequestParam("avatarFile") MultipartFile avatarFile,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String username = getCurrentUsername(session);
        if (username == null) {
            return "redirect:/auth/login";
        }

        // Validate file
        if (avatarFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file ảnh");
            return "redirect:/profile";
        }

        // Check file type
        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("error", "File phải là ảnh (JPG, PNG, GIF)");
            return "redirect:/profile";
        }

        // Check file size (max 5MB)
        if (avatarFile.getSize() > 5 * 1024 * 1024) {
            redirectAttributes.addFlashAttribute("error", "Kích thước file không được vượt quá 5MB");
            return "redirect:/profile";
        }

        try {
            Account updatedAccount = profileService.updateAvatar(username, avatarFile);
            
            // Update session
            session.setAttribute("user", updatedAccount);
            
            redirectAttributes.addFlashAttribute("success", "Cập nhật ảnh đại diện thành công!");
            return "redirect:/profile";
            
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
            
        } catch (Exception e) {
            log.error("Error updating avatar for user: {}", username, e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật ảnh đại diện");
            return "redirect:/profile";
        }
    }

    /**
     * Change password page
     */
    @GetMapping("/change-password")
    public String showChangePassword(HttpSession session, Model model) {
        String username = getCurrentUsername(session);
        if (username == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("profileRequest", new ProfileUpdateRequest());
        return "site/profile/change-password";
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute ProfileUpdateRequest request,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String username = getCurrentUsername(session);
        if (username == null) {
            return "redirect:/auth/login";
        }

        try {
            profileService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
            
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
            return "redirect:/profile";
            
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile/change-password";
            
        } catch (Exception e) {
            log.error("Error changing password for user: {}", username, e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi đổi mật khẩu");
            return "redirect:/profile/change-password";
        }
    }

    /**
     * Get current username from session
     */
    private String getCurrentUsername(HttpSession session) {
        Account user = (Account) session.getAttribute("user");
        return user != null ? user.getUsername() : null;
    }
}