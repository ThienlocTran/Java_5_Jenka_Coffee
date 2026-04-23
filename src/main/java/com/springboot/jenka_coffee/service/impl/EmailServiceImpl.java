package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin.email:${spring.mail.username}}")
    private String adminNotifyEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ================= HELPER METHODS =================

    private String escape(String value, String defaultVal) {
        return HtmlUtils.htmlEscape(value != null ? value : defaultVal);
    }

    private String formatCurrency(BigDecimal total) {
        return total != null
                ? String.format("%,.0f", total.doubleValue()) + " ₫"
                : "N/A";
    }

    private MimeMessageHelper createHelper(MimeMessage message, String to, String subject)
            throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        return helper;
    }

    // ================= EMAIL METHODS =================

    @Override
    @Async
    public void sendActivationEmail(String to, String token, String fullname) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = createHelper(
                    message,
                    to,
                    "Kích hoạt tài khoản Jenka Coffee"
            );

            String safeName = escape(fullname, "");
            String link = baseUrl + "/auth/activate/" + token;

            String html = """
                    <div style="font-family: Arial; max-width:600px;margin:auto;">
                        <h2>Xin chào %s!</h2>
                        <p>Vui lòng kích hoạt tài khoản:</p>
                        <a href="%s">Kích hoạt</a>
                        <p>%s</p>
                    </div>
                    """.formatted(safeName, link, link);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email kích hoạt");
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String token, String fullname) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = createHelper(
                    message,
                    to,
                    "Đặt lại mật khẩu Jenka Coffee"
            );

            String safeName = escape(fullname, "");
            String link = baseUrl + "/auth/reset-password/" + token;

            String html = """
                    <div style="font-family: Arial; max-width:600px;margin:auto;">
                        <h2>Xin chào %s!</h2>
                        <p>Đặt lại mật khẩu:</p>
                        <a href="%s">Reset Password</a>
                        <p>%s</p>
                    </div>
                    """.formatted(safeName, link, link);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email reset password");
        }
    }

    @Override
    @Async
    public void sendNewOrderNotification(String adminEmail, Long orderId,
                                         String customerName, String phone,
                                         String address, BigDecimal total) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = createHelper(
                    message,
                    adminEmail,
                    "[Jenka Coffee] Đơn hàng mới #" + orderId
            );

            String safeName = escape(customerName, "Khách");
            String safePhone = escape(phone, "");
            String safeAddress = escape(address, "");
            String formatted = formatCurrency(total);

            String html = """
                    <h2>Đơn hàng #%d</h2>
                    <p>Khách: %s</p>
                    <p>SĐT: %s</p>
                    <p>Địa chỉ: %s</p>
                    <p>Tổng tiền: %s</p>
                    <a href="%s/admin/order/detail/%d">Xem chi tiết</a>
                    """.formatted(orderId, safeName, safePhone, safeAddress,
                    formatted, baseUrl, orderId);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email đơn hàng");
        }
    }

    @Override
    @Async
    public void sendContactConfirmation(String toEmail, String customerName, String subject) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = createHelper(
                    message,
                    adminNotifyEmail,
                    "[Jenka Coffee] Liên hệ mới từ: " + escape(customerName, "")
            );

            String safeName = escape(customerName, "");
            String safeSubject = escape(subject, "");

            String html = """
                    <h2>Liên hệ mới</h2>
                    <p>Khách: %s</p>
                    <p>Tiêu đề: %s</p>
                    <a href="%s/admin/contacts">Xem</a>
                    """.formatted(safeName, safeSubject, baseUrl);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email liên hệ");
        }
    }
}