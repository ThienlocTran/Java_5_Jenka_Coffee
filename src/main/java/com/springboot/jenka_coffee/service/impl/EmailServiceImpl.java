package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendActivationEmail(String to, String token, String fullname) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Kích hoạt tài khoản Jenka Coffee");

            String activationLink = baseUrl + "/auth/activate/" + token;

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <h2 style="color: #6F4E37;">Xin chào %s!</h2>
                        <p>Cảm ơn bạn đã đăng ký tài khoản tại Jenka Coffee.</p>
                        <p>Vui lòng nhấp vào nút bên dưới để kích hoạt tài khoản của bạn:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #6F4E37; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">
                                Kích hoạt tài khoản
                            </a>
                        </div>
                        <p style="color: #666; font-size: 14px;">Hoặc sao chép link sau vào trình duyệt:</p>
                        <p style="color: #666; font-size: 12px; word-break: break-all;">%s</p>
                        <p style="color: #999; font-size: 12px; margin-top: 30px;">Link này sẽ hết hạn sau 24 giờ.</p>
                        <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
                        <p style="color: #999; font-size: 12px;">Jenka Coffee - Hương vị cà phê đích thực</p>
                    </div>
                    """
                    .formatted(fullname, activationLink, activationLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email kích hoạt", e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String token, String fullname) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Đặt lại mật khẩu Jenka Coffee");

            String resetLink = baseUrl + "/auth/reset-password/" + token;

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <h2 style="color: #6F4E37;">Xin chào %s!</h2>
                        <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                        <p>Vui lòng nhấp vào nút bên dưới để đặt lại mật khẩu:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #6F4E37; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">
                                Đặt lại mật khẩu
                            </a>
                        </div>
                        <p style="color: #666; font-size: 14px;">Hoặc sao chép link sau vào trình duyệt:</p>
                        <p style="color: #666; font-size: 12px; word-break: break-all;">%s</p>
                        <p style="color: #999; font-size: 12px; margin-top: 30px;">Link này sẽ hết hạn sau 1 giờ.</p>
                        <p style="color: #dc3545; font-size: 13px;">Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
                        <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
                        <p style="color: #999; font-size: 12px;">Jenka Coffee - Hương vị cà phê đích thực</p>
                    </div>
                    """
                    .formatted(fullname, resetLink, resetLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu", e);
        }
    }
    @Override
    @Async
    public void sendNewOrderNotification(String adminEmail, Long orderId, String customerName,
                                          String phone, String address, java.math.BigDecimal total) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("[Jenka Coffee] Đơn hàng mới #" + orderId);

            String formatted = total != null
                    ? String.format("%,.0f", total.doubleValue()) + " ₫"
                    : "N/A";

            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;border:1px solid #eee;border-radius:8px;overflow:hidden;">
                      <div style="background:#dc3545;padding:20px 24px;">
                        <h2 style="color:#fff;margin:0;font-size:20px;">🛒 Đơn hàng mới #%d</h2>
                      </div>
                      <div style="padding:24px;">
                        <p style="margin:0 0 16px;color:#333;">Có khách vừa đặt hàng trên <strong>Jenka Coffee</strong>. Chi tiết:</p>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                          <tr><td style="padding:8px 0;color:#666;width:140px;">Mã đơn hàng</td><td style="padding:8px 0;font-weight:bold;color:#212529;">#%d</td></tr>
                          <tr style="background:#f9f9f9;"><td style="padding:8px 6px;color:#666;">Khách hàng</td><td style="padding:8px 6px;font-weight:bold;color:#212529;">%s</td></tr>
                          <tr><td style="padding:8px 0;color:#666;">Số điện thoại</td><td style="padding:8px 0;color:#212529;">%s</td></tr>
                          <tr style="background:#f9f9f9;"><td style="padding:8px 6px;color:#666;">Địa chỉ</td><td style="padding:8px 6px;color:#212529;">%s</td></tr>
                          <tr><td style="padding:8px 0;color:#666;">Tổng tiền</td><td style="padding:8px 0;font-weight:bold;color:#dc3545;font-size:16px;">%s</td></tr>
                        </table>
                        <div style="margin-top:24px;text-align:center;">
                          <a href="%s/admin/order/detail/%d" style="background:#dc3545;color:#fff;padding:12px 28px;text-decoration:none;border-radius:6px;font-weight:bold;display:inline-block;">
                            Xem chi tiết đơn hàng
                          </a>
                        </div>
                      </div>
                      <div style="background:#f8f9fa;padding:12px 24px;text-align:center;font-size:12px;color:#999;">
                        Jenka Coffee — Hương vị cà phê đích thực
                      </div>
                    </div>
                    """.formatted(orderId, orderId, customerName, phone, address, formatted, baseUrl, orderId);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email thông báo đơn hàng", e);
        }
    }
}
