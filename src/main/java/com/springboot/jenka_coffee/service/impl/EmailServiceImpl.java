package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.ConsultationRequest;
import com.springboot.jenka_coffee.entity.Contact;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final ZoneId CONTACT_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CONTACT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm 'ng\u00e0y' dd/MM/yyyy");

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.site-url:${app.base-url:http://localhost:8080}}")
    private String siteUrl;

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

    private String displayValue(Object value) {
        if (value == null) {
            return "Ch\u01b0a cung c\u1ea5p";
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "Ch\u01b0a cung c\u1ea5p" : text;
    }

    private String htmlValue(Object value) {
        return HtmlUtils.htmlEscape(displayValue(value));
    }

    private String htmlAttribute(String value) {
        return HtmlUtils.htmlEscape(value != null ? value : "");
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replaceAll("/+$", "");
    }

    private String adminConsultationsUrl() {
        String publicBaseUrl = normalizeBaseUrl(siteUrl);
        if (publicBaseUrl == null) {
            publicBaseUrl = normalizeBaseUrl(baseUrl);
        }
        if (publicBaseUrl == null) {
            publicBaseUrl = "https://www.jenkacoffee.com";
        }
        return publicBaseUrl + "/admin/consultations";
    }

    private String adminContactsUrl() {
        String publicBaseUrl = normalizeBaseUrl(siteUrl);
        if (publicBaseUrl == null) {
            publicBaseUrl = normalizeBaseUrl(baseUrl);
        }
        if (publicBaseUrl == null) {
            publicBaseUrl = "https://www.jenkacoffee.com";
        }
        return publicBaseUrl + "/admin/contacts";
    }

    private String subjectValue(String value, String defaultVal) {
        String text = value != null ? value.trim() : "";
        if (text.isEmpty()) {
            text = defaultVal;
        }
        return text.replaceAll("[\\r\\n]+", " ").trim();
    }

    private String htmlOptionalValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return HtmlUtils.htmlEscape(text);
    }

    private String htmlOptionalMultiline(Object value) {
        String safeValue = htmlOptionalValue(value);
        if (safeValue == null) {
            return null;
        }
        return safeValue.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "<br>");
    }

    private String contactInfoRow(String label, Object value) {
        String safeValue = htmlOptionalValue(value);
        if (safeValue == null) {
            return "";
        }
        return """
                <tr>
                  <td style="padding:12px 0;color:#7a6253;width:38%%;border-bottom:1px solid #f0e3d7;vertical-align:top;">%s</td>
                  <td style="padding:12px 0;color:#2b1712;font-weight:700;border-bottom:1px solid #f0e3d7;vertical-align:top;word-break:break-word;">%s</td>
                </tr>
                """.formatted(HtmlUtils.htmlEscape(label), safeValue);
    }

    private String contactMessageBlock(Object value) {
        String safeMessage = htmlOptionalMultiline(value);
        if (safeMessage == null) {
            return "";
        }
        return """
                <div style="margin-top:22px;background:#fffaf4;border:1px solid #f0dfca;border-radius:12px;padding:18px 20px;">
                  <div style="font-size:13px;font-weight:800;letter-spacing:.05em;text-transform:uppercase;color:#9f1d20;margin-bottom:10px;">N\u1ed9i dung li\u00ean h\u1ec7</div>
                  <div style="font-size:15px;line-height:1.7;color:#2b1712;word-break:break-word;">%s</div>
                </div>
                """.formatted(safeMessage);
    }

    private String contactButton(String href, String label, String background, String color, String borderColor) {
        if (isBlank(href)) {
            return "";
        }
        return """
                <a class="button" href="%s" style="display:inline-block;margin:0 8px 10px 0;padding:12px 16px;border-radius:8px;background:%s;border:1px solid %s;color:%s;text-decoration:none;font-size:14px;font-weight:800;line-height:1.2;">%s</a>
                """.formatted(
                htmlAttribute(href),
                htmlAttribute(background),
                htmlAttribute(borderColor),
                htmlAttribute(color),
                HtmlUtils.htmlEscape(label)
        );
    }

    private String contactPhoneHref(String phone) {
        if (isBlank(phone)) {
            return null;
        }
        String normalized = phone.replaceAll("[^\\d+]", "");
        return normalized.length() >= 3 ? "tel:" + normalized : null;
    }

    private String contactMailHref(String email) {
        String normalized = normalizeEmail(email);
        return isValidEmail(normalized) ? "mailto:" + normalized : null;
    }

    private String formatContactSentAt() {
        return LocalDateTime.now(CONTACT_TIME_ZONE).format(CONTACT_TIME_FORMATTER) + " (Asia/Ho_Chi_Minh)";
    }

    private String buildContactNotificationHtml(Contact contact, String adminLink, String businessEmail) {
        String rows = ""
                + contactInfoRow("M\u00e3 li\u00ean h\u1ec7", contact.getId() != null ? "#" + contact.getId() : null)
                + contactInfoRow("H\u1ecd t\u00ean kh\u00e1ch", contact.getFullName())
                + contactInfoRow("S\u1ed1 \u0111i\u1ec7n tho\u1ea1i", contact.getPhone())
                + contactInfoRow("Email", contact.getEmail())
                + contactInfoRow("Ti\u00eau \u0111\u1ec1 li\u00ean h\u1ec7", contact.getSubject())
                + contactInfoRow("Th\u1eddi gian g\u1eedi", formatContactSentAt());

        String buttons = ""
                + contactButton(adminLink, "Xem trong trang qu\u1ea3n tr\u1ecb", "#9f1d20", "#ffffff", "#9f1d20")
                + contactButton(contactPhoneHref(contact.getPhone()), "G\u1ecdi kh\u00e1ch", "#ffffff", "#9f1d20", "#9f1d20")
                + contactButton(contactMailHref(contact.getEmail()), "G\u1eedi email", "#ffffff", "#9f1d20", "#9f1d20");

        return """
                <!doctype html>
                <html>
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <style>
                    @media only screen and (max-width: 640px) {
                      .container { width: 100%% !important; }
                      .outer-pad { padding: 18px 10px !important; }
                      .content { padding: 22px 18px !important; }
                      .header { padding: 24px 18px !important; }
                      .button { display: block !important; margin: 0 0 10px 0 !important; text-align: center !important; }
                    }
                  </style>
                </head>
                <body style="margin:0;padding:0;background:#f6efe7;font-family:Arial,Helvetica,sans-serif;color:#2b1712;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="width:100%%;background:#f6efe7;border-collapse:collapse;">
                    <tr>
                      <td class="outer-pad" align="center" style="padding:32px 14px;">
                        <table role="presentation" class="container" width="640" cellspacing="0" cellpadding="0" border="0" style="width:640px;max-width:640px;border-collapse:separate;border-spacing:0;">
                          <tr>
                            <td class="header" style="background:#9f1d20;color:#ffffff;padding:28px 30px;border-radius:18px 18px 0 0;">
                              <div style="font-size:14px;font-weight:800;letter-spacing:.08em;text-transform:uppercase;color:#f7d9bf;">Jenka Coffee</div>
                              <h1 style="margin:10px 0 0;font-size:25px;line-height:1.3;color:#ffffff;font-weight:800;">C\u00f3 li\u00ean h\u1ec7 m\u1edbi t\u1eeb website Jenka Coffee</h1>
                            </td>
                          </tr>
                          <tr>
                            <td class="content" style="background:#ffffff;border-left:1px solid #ead8c6;border-right:1px solid #ead8c6;padding:28px 30px;">
                              <p style="margin:0 0 20px;font-size:15px;line-height:1.7;color:#5f493c;">Kh\u00e1ch h\u00e0ng v\u1eeba g\u1eedi th\u00f4ng tin li\u00ean h\u1ec7 t\u1eeb website. Vui l\u00f2ng ki\u1ec3m tra v\u00e0 ph\u1ea3n h\u1ed3i s\u1edbm.</p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="width:100%%;border-collapse:collapse;font-size:15px;">
                                %s
                              </table>
                              %s
                              <div style="margin-top:24px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#fff7ed;border:1px solid #ead8c6;border-top:0;border-radius:0 0 18px 18px;padding:20px 30px;color:#6e5748;font-size:13px;line-height:1.6;">
                              <p style="margin:0 0 6px;font-weight:700;color:#4b2a20;">Email \u0111\u01b0\u1ee3c g\u1eedi t\u1ef1 \u0111\u1ed9ng t\u1eeb website Jenka Coffee</p>
                              <p style="margin:0;">Hotline: <a href="tel:0817909090" style="color:#9f1d20;text-decoration:none;font-weight:700;">0817 909 090</a> &nbsp;|&nbsp; Email: <span style="color:#2b1712;font-weight:700;">%s</span></p>
                              <p style="margin:10px 0 0;color:#8a6d5a;">B\u1ea3o m\u1eadt: email n\u00e0y ch\u1ee9a th\u00f4ng tin kh\u00e1ch h\u00e0ng. Ch\u1ec9 chia s\u1ebb cho nh\u00f3m ph\u1ee5 tr\u00e1ch x\u1eed l\u00fd li\u00ean h\u1ec7.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(rows, contactMessageBlock(contact.getMessage()), buttons, htmlValue(businessEmail));
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
    public void sendContactConfirmation(Contact contact) {
        if (contact == null) {
            log.warn("Contact email skipped: contact is null");
            return;
        }
        String adminEmail = resolveAdminEmail();
        if (adminEmail == null) {
            log.warn("Admin email not configured or invalid; contact email skipped for id={}", contact.getId());
            return;
        }
        String senderEmail = resolveSenderEmail();
        if (senderEmail == null) {
            log.warn("Mail sender not configured; contact email skipped for id={}", contact.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            String customerName = subjectValue(contact.getFullName(), "Khach");
            String adminLink = adminContactsUrl();
            String html = buildContactNotificationHtml(contact, adminLink, senderEmail);
            MimeMessageHelper helper = createHelper(
                    message,
                    adminEmail,
                    "[Jenka Coffee] Li\u00ean h\u1ec7 m\u1edbi t\u1eeb: " + customerName,
                    senderEmail
            );

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logMailFailure("sendContactConfirmation id=" + contact.getId(), adminEmail, e);
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
            String phone = displayValue(consultation.getContactPhone());
            String adminLink = adminConsultationsUrl();
            MimeMessageHelper helper = createHelper(
                    message,
                    adminEmail,
                    "[Jenka Coffee] Kh\u00e1ch m\u1edbi c\u1ea7n t\u01b0 v\u1ea5n - " + phone,
                    senderEmail
            );

            String textBody = """
                    [Jenka Coffee]
                    Kh\u00e1ch m\u1edbi c\u1ea7n t\u01b0 v\u1ea5n

                    Kh\u00e1ch v\u1eeba g\u1eedi y\u00eau c\u1ea7u t\u01b0 v\u1ea5n t\u1eeb website.
                    Vui l\u00f2ng g\u1ecdi l\u1ea1i s\u1edbm \u0111\u1ec3 t\u0103ng kh\u1ea3 n\u0103ng ch\u1ed1t \u0111\u01a1n.

                    TH\u00d4NG TIN KH\u00c1CH
                    H\u1ecd t\u00ean: %s
                    S\u1ed1 \u0111i\u1ec7n tho\u1ea1i/Zalo: %s
                    Email: Ch\u01b0a cung c\u1ea5p

                    NHU C\u1ea6U
                    Lo\u1ea1i nhu c\u1ea7u: %s
                    H\u1ea1ng m\u1ee5c quan t\u00e2m: %s
                    S\u1ea3n ph\u1ea9m c\u1ee5 th\u1ec3: %s
                    Ng\u00e2n s\u00e1ch: %s
                    Ghi ch\u00fa: %s

                    NGU\u1ed2N
                    Trang g\u1eedi: %s
                    URL: %s
                    Source: %s
                    Th\u1eddi gian: %s

                    M\u1edf trang qu\u1ea3n tr\u1ecb: %s
                    """.formatted(
                    displayValue(consultation.getFullName()),
                    phone,
                    displayValue(consultation.getNeedType()),
                    displayValue(consultation.getInterest()),
                    displayValue(consultation.getProductName()),
                    displayValue(consultation.getBudget()),
                    displayValue(consultation.getNote()),
                    displayValue(consultation.getPageTitle()),
                    displayValue(consultation.getPageUrl()),
                    displayValue(consultation.getSource()),
                    displayValue(consultation.getCreatedAt()),
                    adminLink
            );

            String phoneHref = phone.replaceAll("[^\\d+]", "");
            String htmlBody = """
                    <div style="margin:0;padding:0;background:#f6f1ea;font-family:Arial,Helvetica,sans-serif;color:#2d1b12;">
                      <div style="max-width:680px;margin:0 auto;padding:28px 16px;">
                        <div style="background:#ffffff;border:1px solid #eadfd2;border-radius:16px;overflow:hidden;box-shadow:0 10px 28px rgba(63,38,24,.10);">
                          <div style="background:#4a2518;color:#fff;padding:26px 30px;">
                            <div style="font-size:14px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#f3d4ae;">Jenka Coffee</div>
                            <h1 style="margin:10px 0 6px;font-size:26px;line-height:1.25;color:#fff;">Kh\u00e1ch m\u1edbi c\u1ea7n t\u01b0 v\u1ea5n</h1>
                            <p style="margin:0;color:#f7e8d7;font-size:15px;line-height:1.6;">Vui l\u00f2ng g\u1ecdi l\u1ea1i s\u1edbm \u0111\u1ec3 ch\u1ed1t nhu c\u1ea7u.</p>
                          </div>
                          <div style="padding:26px 30px;">
                            <div style="background:#fff7ef;border:1px solid #f0dfca;border-radius:12px;padding:18px 20px;margin-bottom:22px;">
                              <div style="font-size:13px;color:#8a6550;font-weight:700;text-transform:uppercase;letter-spacing:.06em;">S\u1ed1 \u0111i\u1ec7n tho\u1ea1i/Zalo</div>
                              <a href="tel:%s" style="display:inline-block;margin-top:8px;color:#b84f1f;font-size:28px;font-weight:800;text-decoration:none;line-height:1.2;">%s</a>
                            </div>

                            <h2 style="margin:0 0 12px;color:#4a2518;font-size:18px;line-height:1.3;">TH\u00d4NG TIN KH\u00c1CH</h2>
                            <table role="presentation" style="width:100%%;border-collapse:collapse;margin-bottom:24px;font-size:15px;">
                              <tr><td style="padding:10px 0;color:#7b6758;width:42%%;border-bottom:1px solid #f0e5d8;">H\u1ecd t\u00ean</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;">S\u1ed1 \u0111i\u1ec7n tho\u1ea1i/Zalo</td><td style="padding:10px 0;color:#b84f1f;font-weight:800;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;">Email</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">Ch\u01b0a cung c\u1ea5p</td></tr>
                            </table>

                            <h2 style="margin:0 0 12px;color:#4a2518;font-size:18px;line-height:1.3;">NHU C\u1ea6U</h2>
                            <table role="presentation" style="width:100%%;border-collapse:collapse;margin-bottom:24px;font-size:15px;">
                              <tr><td style="padding:10px 0;color:#7b6758;width:42%%;border-bottom:1px solid #f0e5d8;">Lo\u1ea1i nhu c\u1ea7u</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;">H\u1ea1ng m\u1ee5c quan t\u00e2m</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;">S\u1ea3n ph\u1ea9m c\u1ee5 th\u1ec3</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;">Ng\u00e2n s\u00e1ch</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;vertical-align:top;">Ghi ch\u00fa</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;line-height:1.6;">%s</td></tr>
                            </table>

                            <h2 style="margin:0 0 12px;color:#4a2518;font-size:18px;line-height:1.3;">NGU\u1ed2N</h2>
                            <table role="presentation" style="width:100%%;border-collapse:collapse;margin-bottom:26px;font-size:15px;">
                              <tr><td style="padding:10px 0;color:#7b6758;width:42%%;border-bottom:1px solid #f0e5d8;">Trang g\u1eedi</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;">URL</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;word-break:break-word;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;">Source</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                              <tr><td style="padding:10px 0;color:#7b6758;border-bottom:1px solid #f0e5d8;">Th\u1eddi gian</td><td style="padding:10px 0;color:#2d1b12;font-weight:700;border-bottom:1px solid #f0e5d8;">%s</td></tr>
                            </table>

                            <div style="text-align:center;margin:28px 0 8px;">
                              <a href="%s" style="display:inline-block;background:#b84f1f;color:#ffffff;text-decoration:none;padding:13px 22px;border-radius:8px;font-size:15px;font-weight:800;">M\u1edf trang qu\u1ea3n tr\u1ecb</a>
                            </div>
                            <p style="margin:18px 0 0;color:#8a6550;text-align:center;font-size:13px;line-height:1.5;">Kh\u00e1ch v\u1eeba g\u1eedi y\u00eau c\u1ea7u t\u01b0 v\u1ea5n t\u1eeb website. Vui l\u00f2ng g\u1ecdi l\u1ea1i s\u1edbm \u0111\u1ec3 t\u0103ng kh\u1ea3 n\u0103ng ch\u1ed1t \u0111\u01a1n.</p>
                          </div>
                        </div>
                      </div>
                    </div>
                    """.formatted(
                    htmlAttribute(phoneHref),
                    htmlValue(phone),
                    htmlValue(consultation.getFullName()),
                    htmlValue(phone),
                    htmlValue(consultation.getNeedType()),
                    htmlValue(consultation.getInterest()),
                    htmlValue(consultation.getProductName()),
                    htmlValue(consultation.getBudget()),
                    htmlValue(consultation.getNote()),
                    htmlValue(consultation.getPageTitle()),
                    htmlValue(consultation.getPageUrl()),
                    htmlValue(consultation.getSource()),
                    htmlValue(consultation.getCreatedAt()),
                    htmlAttribute(adminLink)
            );

            helper.setText(textBody, htmlBody);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logMailFailure("sendConsultationNotification id=" + consultation.getId(), adminEmail, e);
        }
    }
}
