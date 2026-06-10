package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.ConsultationRequest;
import com.springboot.jenka_coffee.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${app.admin.email:${spring.mail.username:}}")
    private String adminNotifyEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private String escape(String value, String defaultVal) {
        return HtmlUtils.htmlEscape(value != null ? value : defaultVal);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String email = value.trim();
        return email.isEmpty() ? null : email;
    }

    private boolean isValidEmail(String value) {
        String email = normalizeEmail(value);
        if (email == null) {
            return false;
        }
        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            return email.equals(address.getAddress());
        } catch (AddressException ex) {
            return false;
        }
    }

    private String resolveConfiguredEmail(String value) {
        String email = normalizeEmail(value);
        return isValidEmail(email) ? email : null;
    }

    private String resolveSenderEmail() {
        String configuredFrom = normalizeEmail(fromEmail);
        if (isValidEmail(configuredFrom)) {
            return configuredFrom;
        }
        if (!isBlank(configuredFrom)) {
            log.warn("Configured mail from address is invalid; falling back to spring.mail.username");
        }

        String username = normalizeEmail(mailUsername);
        if (isValidEmail(username)) {
            return username;
        }
        return null;
    }

    private String resolveAdminEmail() {
        return resolveConfiguredEmail(adminNotifyEmail);
    }

    private String value(Object value) {
        if (value == null) {
            return "N/A";
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "N/A" : text;
    }

    private String formatCurrency(BigDecimal total) {
        return total != null
                ? String.format("%,.0f", total.doubleValue()) + " ₫"
                : "N/A";
    }

    private MimeMessageHelper createHelper(MimeMessage message, String to, String subject)
            throws MessagingException {
        String senderEmail = resolveSenderEmail();
        if (senderEmail == null) {
            throw new MessagingException("Mail sender not configured");
        }
        return createHelper(message, to, subject, senderEmail);
    }

    private MimeMessageHelper createHelper(MimeMessage message, String to, String subject, String senderEmail)
            throws MessagingException {
        String recipientEmail = resolveConfiguredEmail(to);
        if (recipientEmail == null) {
            throw new MessagingException("Invalid recipient email");
        }

        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        try {
            helper.setFrom(senderEmail, "Jenka Coffee");
        } catch (UnsupportedEncodingException ex) {
            helper.setFrom(senderEmail);
        }
        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        return helper;
    }

    private void logMailFailure(String action, String to, Exception e) {
        log.error("[EMAIL FAIL] {} | to={} | cause={}. Check mail username/from config and app password.",
                action, to, e.getMessage(), e);
    }

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
                    <div style="font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:auto;">
                        <h2>Xin chào %s!</h2>
                        <p>Vui lòng kích hoạt tài khoản Jenka Coffee của bạn:</p>
                        <a href="%s">Kích hoạt tài khoản</a>
                        <p>%s</p>
                    </div>
                    """.formatted(safeName, link, link);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logMailFailure("sendActivationEmail", to, e);
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
                    <div style="font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:auto;">
                        <h2>Xin chào %s!</h2>
                        <p>Đặt lại mật khẩu Jenka Coffee:</p>
                        <a href="%s">Đặt lại mật khẩu</a>
                        <p>%s</p>
                    </div>
                    """.formatted(safeName, link, link);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logMailFailure("sendPasswordResetEmail", to, e);
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
                    <div style="margin:0;padding:0;background:#f6f1ea;font-family:Arial,Helvetica,sans-serif;color:#24170f;">
                      <div style="max-width:640px;margin:0 auto;padding:28px 16px;">
                        <div style="background:#ffffff;border:1px solid #eadfd2;border-radius:16px;overflow:hidden;">
                          <div style="background:#3f2618;color:#fff;padding:22px 28px;">
                            <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#e8cfae;">Jenka Coffee</div>
                            <h1 style="margin:8px 0 0;font-size:24px;line-height:1.3;">Có đơn hàng mới #%d</h1>
                          </div>
                          <div style="padding:26px 28px;">
                            <p style="margin:0 0 18px;font-size:16px;">Một khách hàng vừa đặt mua trên website. Vui lòng kiểm tra và liên hệ xác nhận sớm.</p>
                            <table style="width:100%%;border-collapse:collapse;font-size:15px;">
                              <tr><td style="padding:10px 0;color:#7b6758;">Khách hàng</td><td style="padding:10px 0;text-align:right;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;">Số điện thoại</td><td style="padding:10px 0;text-align:right;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;">Địa chỉ</td><td style="padding:10px 0;text-align:right;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:14px 0;border-top:1px solid #eadfd2;color:#7b6758;">Tổng tiền</td><td style="padding:14px 0;border-top:1px solid #eadfd2;text-align:right;font-size:20px;font-weight:800;color:#b85c20;">%s</td></tr>
                            </table>
                            <div style="margin-top:24px;">
                              <a href="%s/admin/order/detail/%d" style="display:inline-block;background:#b85c20;color:#fff;text-decoration:none;padding:12px 18px;border-radius:8px;font-weight:700;">Xem chi tiết đơn</a>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                    """.formatted(orderId, safeName, safePhone, safeAddress, formatted, baseUrl, orderId);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logMailFailure("sendNewOrderNotification orderId=" + orderId, adminEmail, e);
        }
    }

    @Override
    @Async
    public void sendOrderConfirmation(String customerEmail, String customerName, Long orderId,
                                      String orderCode, String phone, String address, BigDecimal total) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = createHelper(
                    message,
                    customerEmail,
                    "Cảm ơn bạn đã mua hàng tại Jenka Coffee"
            );

            String safeName = escape(customerName, "bạn");
            String safeOrderCode = escape(orderCode, "#" + orderId);
            String safePhone = escape(phone, "");
            String safeAddress = escape(address, "");
            String formatted = formatCurrency(total);
            String orderUrl = baseUrl + "/orders/" + safeOrderCode;

            String html = """
                    <div style="margin:0;padding:0;background:#f6f1ea;font-family:Arial,Helvetica,sans-serif;color:#24170f;">
                      <div style="max-width:640px;margin:0 auto;padding:28px 16px;">
                        <div style="text-align:center;margin-bottom:18px;">
                          <div style="font-size:22px;font-weight:800;color:#3f2618;">Jenka Coffee</div>
                          <div style="font-size:13px;color:#8a7665;margin-top:4px;">Máy pha, hạt cà phê và dịch vụ tận tâm</div>
                        </div>
                        <div style="background:#ffffff;border:1px solid #eadfd2;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(63,38,24,.08);">
                          <div style="background:#3f2618;color:#fff;padding:30px 28px;text-align:center;">
                            <div style="display:inline-block;background:#e8cfae;color:#3f2618;border-radius:999px;padding:7px 12px;font-size:13px;font-weight:700;">Đặt hàng thành công</div>
                            <h1 style="margin:16px 0 8px;font-size:28px;line-height:1.25;">Cảm ơn %s đã mua hàng</h1>
                            <p style="margin:0;color:#f4e7d6;font-size:15px;">Jenka Coffee đã nhận đơn của bạn và sẽ liên hệ xác nhận trong thời gian sớm nhất.</p>
                          </div>
                          <div style="padding:26px 28px;">
                            <div style="background:#fff8f0;border:1px solid #f0dfca;border-radius:12px;padding:16px;margin-bottom:20px;">
                              <div style="font-size:13px;color:#8a7665;">Mã đơn hàng</div>
                              <div style="font-size:24px;font-weight:800;color:#b85c20;margin-top:4px;">%s</div>
                            </div>
                            <table style="width:100%%;border-collapse:collapse;font-size:15px;">
                              <tr><td style="padding:10px 0;color:#7b6758;">Số điện thoại</td><td style="padding:10px 0;text-align:right;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;">Địa chỉ giao hàng</td><td style="padding:10px 0;text-align:right;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:14px 0;border-top:1px solid #eadfd2;color:#7b6758;">Tổng thanh toán</td><td style="padding:14px 0;border-top:1px solid #eadfd2;text-align:right;font-size:22px;font-weight:800;color:#b85c20;">%s</td></tr>
                            </table>
                            <div style="margin-top:24px;text-align:center;">
                              <a href="%s" style="display:inline-block;background:#b85c20;color:#fff;text-decoration:none;padding:12px 18px;border-radius:8px;font-weight:700;">Xem đơn hàng</a>
                            </div>
                            <p style="margin:22px 0 0;color:#7b6758;font-size:14px;line-height:1.6;">Nếu thông tin chưa đúng, bạn chỉ cần phản hồi email này hoặc liên hệ Jenka Coffee để được hỗ trợ.</p>
                          </div>
                        </div>
                      </div>
                    </div>
                    """.formatted(safeName, safeOrderCode, safePhone, safeAddress, formatted, orderUrl);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logMailFailure("sendOrderConfirmation orderId=" + orderId, customerEmail, e);
        }
    }

    @Override
    @Async
    public void sendContactConfirmation(String toEmail, String customerName, String subject) {
        String adminEmail = resolveAdminEmail();
        if (adminEmail == null) {
            log.warn("Admin email not configured or invalid; contact email skipped");
            return;
        }
        String senderEmail = resolveSenderEmail();
        if (senderEmail == null) {
            log.warn("Mail sender not configured; contact email skipped");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = createHelper(
                    message,
                    adminEmail,
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
        } catch (MessagingException | MailException e) {
            logMailFailure("sendContactConfirmation", adminEmail, e);
        }
    }

    @Override
    @Async
    public void sendConsultationNotification(ConsultationRequest consultation) {
        if (consultation == null) {
            log.warn("Consultation email skipped: request is null");
            return;
        }
        String adminEmail = resolveAdminEmail();
        if (adminEmail == null) {
            log.warn("Admin email not configured or invalid; consultation email skipped for id={}", consultation.getId());
            return;
        }
        String senderEmail = resolveSenderEmail();
        if (senderEmail == null) {
            log.warn("Mail sender not configured; consultation email skipped for id={}", consultation.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            String phone = value(consultation.getContactPhone());
            MimeMessageHelper helper = createHelper(
                    message,
                    adminEmail,
                    "[Jenka Coffee] Khach moi can tu van - " + phone,
                    senderEmail
            );

            String adminLink = isBlank(baseUrl) ? "N/A" : baseUrl + "/admin/consultations";
            String body = """
                    Khach moi can tu van tu Jenka Coffee

                    Khach vua gui yeu cau tu van. Vui long goi lai som.

                    Thong tin khach:
                    - Ho ten: %s
                    - So dien thoai/Zalo: %s
                    - Email: N/A

                    Nhu cau:
                    - Loai nhu cau: %s
                    - San pham/hang muc quan tam: %s
                    - San pham cu the: %s
                    - Ngan sach: %s
                    - Ghi chu: %s

                    Nguon:
                    - Tieu de trang: %s
                    - Trang gui: %s
                    - Source: %s
                    - Thoi gian: %s

                    Hanh dong:
                    Vui long goi lai khach trong thoi gian som nhat.

                    Admin:
                    %s
                    """.formatted(
                    value(consultation.getFullName()),
                    phone,
                    value(consultation.getNeedType()),
                    value(consultation.getInterest()),
                    value(consultation.getProductName()),
                    value(consultation.getBudget()),
                    value(consultation.getNote()),
                    value(consultation.getPageTitle()),
                    value(consultation.getPageUrl()),
                    value(consultation.getSource()),
                    value(consultation.getCreatedAt()),
                    adminLink
            );

            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logMailFailure("sendConsultationNotification id=" + consultation.getId(), adminEmail, e);
        }
    }
}
