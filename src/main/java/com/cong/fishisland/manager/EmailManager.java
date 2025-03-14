package com.cong.fishisland.manager;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.Random;

@Service
@Slf4j
public class EmailManager {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送验证码邮件
     * @param sendEmail 接收邮箱
     * @return 发送的验证码
     */
    public String sendVerificationCode(String sendEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // 使用MimeMessageHelper来构建邮件
            MimeMessageHelper mailMessage = new MimeMessageHelper(mimeMessage, true); // true表示支持HTML
            // 主题
            mailMessage.setSubject("【摸鱼岛】验证码邮件");
            // 生成6位随机验证码
            String code = generateCode();
            // 邮件内容
            // 邮件内容，使用HTML格式
            String emailContent = "<html><body>"
                    + "<h2>亲爱的用户，恭喜你成功注册摸鱼岛！🎉</h2>"
                    + "<p>为了确保账户安全，请输入以下验证码完成验证：</p>"
                    + "<h3 style='color: #FF6347;'>验证码：<strong>" + code + "</strong></h3>"
                    + "<p>该验证码将在 60 秒内有效，请尽快完成验证。</p>"
                    + "<p>如果您没有发起此请求，请忽略此邮件。</p>"
                    + "<br>"
                    + "<p>更多信息请访问我们的官网：</p>"
                    + "<a href='https://fish.codebug.icu/' target='_blank'>摸鱼岛官网</a>"
                    + "</body></html>";
            // 设置邮件内容，true表示HTML格式
            mailMessage.setText(emailContent, true);
            log.info("您收到的验证码是：" + code);
            // 目标邮箱
            mailMessage.setTo(sendEmail);
            // 发件人邮箱
            mailMessage.setFrom(from);
            // 发送邮件
            mailSender.send(mimeMessage);
            return code;
        } catch (Exception e) {
            e.printStackTrace();
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
