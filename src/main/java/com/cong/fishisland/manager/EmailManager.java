package com.cong.fishisland.manager;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EmailManager {

    @Resource
    private JavaMailSender mailSender;

    @Resource
    StringRedisTemplate stringRedisTemplate;
    private static final String EMAIL_CODE_PREFIX = "email:code:";

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送验证码邮件
     *
     * @param sendEmail 接收邮箱
     */
    @Async
    public void sendVerificationCode(String sendEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // 使用MimeMessageHelper来构建邮件 true表示支持HTM
            MimeMessageHelper mailMessage = new MimeMessageHelper(mimeMessage, true);
            // 主题
            mailMessage.setSubject("【摸鱼岛】验证码邮件");
            // 生成6位随机验证码
            String code = generateCode();
            // 邮件内容
            // 邮件内容，使用HTML格式
            String emailContent ="<html>" +
                    "<head>" +
                    "  <meta charset='UTF-8'>" +
                    "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "  <style>" +
                    "    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; margin: 0; padding: 0; color: #333; }" +
                    "    .container { max-width: 600px; margin: 30px auto; background-color: #ffffff; padding: 25px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); }" +
                    "    .header { text-align: center; margin-bottom: 20px; }" +
                    "    .header h2 { color: #333; font-size: 24px; font-weight: bold; }" +
                    "    .content { font-size: 16px; line-height: 1.6; color: #555; }" +
                    "    .code { font-size: 28px; font-weight: bold; color: #FF6347; padding: 10px 20px; background-color: #f4f4f4; border-radius: 8px; margin: 20px 0; text-align: center; display: block; }" +
                    "    .footer { margin-top: 30px; text-align: center; font-size: 14px; color: #888; }" +
                    "    .footer p { margin-bottom: 10px; }" +
                    "    a { color: #1E90FF; text-decoration: none; font-weight: bold; }" +
                    "    a:hover { text-decoration: underline; }" +
                    "    .cta-button { background-color: #007BFF; color: black; text-align: center; padding: 12px 20px; font-size: 16px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }" +
                    "    .cta-button:hover { background-color: #0056b3; }" +  // Darker blue on hover
                    "  </style>" +
                    "</head>" +
                    "<body>" +
                    "  <div class='container'>" +
                    "    <div class='header'>" +
                    "      <h2>亲爱的用户，您好！ 🎉</h2>" +
                    "    </div>" +
                    "    <div class='content'>" +
                    "      <p>为了确保您的账户安全，我们需要您输入以下验证码完成验证：</p>" +
                    "      <p class='code'>验证码：<strong>" + code + "</strong></p>" +
                    "      <p>请注意：该验证码将在 <strong>60 秒</strong> 内过期，请尽快完成验证。</p>" +
                    "      <p>如果您未发起此请求，或对此操作不感兴趣，请忽略此邮件。</p>" +
                    "      <p>为确保您的安全，建议不要将验证码泄露给他人。</p>" +
                    "    </div>" +
                    "    <div class='footer'>" +
                    "      <p>如有任何疑问或需要帮助，请访问我们的官方帮助中心。</p>" +
                    "      <p><a href='https://yucoder.cn' target='_blank' class='cta-button'>访问摸鱼岛官网</a></p>" +
                    "      <p>© 2025 摸鱼岛. 保留所有权利。</p>" +
                    "    </div>" +
                    "  </div>" +
                    "</body>" +
                    "</html>";

            // 设置邮件内容，true表示HTML格式
            mailMessage.setText(emailContent, true);
            log.info("您收到的验证码是：" + code);
            // 目标邮箱
            mailMessage.setTo(sendEmail);
            // 发件人邮箱
            mailMessage.setFrom(from);
            // 发送邮件
            mailSender.send(mimeMessage);
            // 存入 Redis，1 分钟有效
            stringRedisTemplate.opsForValue().set(EMAIL_CODE_PREFIX + sendEmail, code, 1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败");
        }
    }

    /**
     * 生成6位随机验证码
     */
    private String generateCode() {
        StringBuilder str = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            str.append(random.nextInt(10));
        }
        return str.toString();
    }
}
