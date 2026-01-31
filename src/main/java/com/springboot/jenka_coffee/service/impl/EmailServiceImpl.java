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
}
