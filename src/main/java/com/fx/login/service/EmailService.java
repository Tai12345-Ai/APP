package com.fx.login.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;

    public EmailService(JavaMailSender emailSender, TemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = "dvhung090705@gmail.com"; // Lấy từ application.properties
    }

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực khôi phục mật khẩu");
        message.setText("Mã xác thực của bạn là: " + code);
        mailSender.send(message);
    }

    public void sendAccountVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực tài khoản");
        message.setText("Mã xác thực của bạn là: " + code);
        mailSender.send(message);
    }

    public void sendPasswordVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực đổi mật khẩu");
        message.setText("Mã xác thực của bạn là: " + code);
        mailSender.send(message);
    }

    public void sendInfoVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực thông tin người dùng");
        message.setText("Mã xác thực của bạn là: " + code);
        mailSender.send(message);
    }

    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        try {
            // true: multipart message (cho phép HTML, inline elements, attachments)
            // "UTF-8": character encoding
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true: chỉ định nội dung là HTML

            emailSender.send(mimeMessage);
            System.out.println("HTML Email sent successfully to " + toEmail + " with subject: " + subject);

        } catch (MessagingException e) {
            System.err.println("Error sending HTML email to " + toEmail + ". Subject: " + subject + ". Error: " + e.getMessage());
        }
    }

    public void sendNotificationEmail(String to, String subject, String content,
                                      String linkUrl, String type) throws MessagingException {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            // Create HTML content directly instead of using a template
            String htmlContent = createNotificationHtmlContent(subject, content, linkUrl, type);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            System.out.println("Notification email sent successfully to " + to);

        } catch (Exception e) {
            System.err.println("Error sending notification email to " + to + ": " + e.getMessage());
            throw new MessagingException("Không thể gửi email đến " + to + ": " + e.getMessage());
        }
    }

    private String createNotificationHtmlContent(String subject, String content, String linkUrl, String type) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("    <meta charset=\"UTF-8\">")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("    <title>").append(subject).append("</title>")
                .append("    <style>")
                .append("        body {")
                .append("            font-family: Arial, sans-serif;")
                .append("            line-height: 1.6;")
                .append("            color: #333;")
                .append("            margin: 0;")
                .append("            padding: 0;")
                .append("        }")
                .append("        .container {")
                .append("            max-width: 600px;")
                .append("            margin: 0 auto;")
                .append("            padding: 20px;")
                .append("            border: 1px solid #ddd;")
                .append("            border-radius: 5px;")
                .append("        }")
                .append("        .header {")
                .append("            background-color: #4A86E8;")
                .append("            color: white;")
                .append("            padding: 15px;")
                .append("            text-align: center;")
                .append("            border-radius: 5px 5px 0 0;")
                .append("        }")
                .append("        .content {")
                .append("            padding: 20px;")
                .append("        }")
                .append("        .footer {")
                .append("            background-color: #f2f2f2;")
                .append("            padding: 10px;")
                .append("            text-align: center;")
                .append("            border-radius: 0 0 5px 5px;")
                .append("            font-size: 12px;")
                .append("            color: #777;")
                .append("        }")
                .append("        .button {")
                .append("            display: inline-block;")
                .append("            background-color: #4CAF50;")
                .append("            color: white;")
                .append("            padding: 10px 20px;")
                .append("            text-decoration: none;")
                .append("            border-radius: 5px;")
                .append("            margin-top: 20px;")
                .append("        }")
                .append("        .notification-type {")
                .append("            font-weight: bold;")
                .append("            margin-bottom: 10px;")
                .append("            color: #555;")
                .append("        }")
                .append("    </style>")
                .append("</head>")
                .append("<body>")
                .append("    <div class=\"container\">")
                .append("        <div class=\"header\">")
                .append("            <h2>Thông báo từ Hệ thống Quản lý Chung cư</h2>")
                .append("        </div>")
                .append("        <div class=\"content\">")
                .append("            <div class=\"notification-type\">Loại thông báo: ").append(type).append("</div>")
                .append("            <div>").append(content).append("</div>");

        if (linkUrl != null && !linkUrl.isEmpty()) {
            html.append("            <div><a class=\"button\" href=\"").append(linkUrl).append("\">Xem chi tiết</a></div>");
        }

        html.append("        </div>")
                .append("        <div class=\"footer\">")
                .append("            <p>Email này được gửi tự động từ Hệ thống Quản lý Chung cư. Vui lòng không trả lời email này.</p>")
                .append("            <p>© 2023 Hệ thống Quản lý Chung cư</p>")
                .append("        </div>")
                .append("    </div>")
                .append("</body>")
                .append("</html>");

        return html.toString();
    }

    public void sendSimpleEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, false);

        emailSender.send(message);
    }
}
