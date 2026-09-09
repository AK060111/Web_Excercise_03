package vn.iotstar.service;

import java.util.Properties;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import vn.iotstar.config.AppConfig;

public class MailService {
    public void sendOtp(String email, String otp) {
        send(email,otp,"Kích hoạt tài khoản ServletCRUDMVC","Mã OTP kích hoạt tài khoản của bạn: ");
    }
    public void sendResetOtp(String email,String otp) {
        send(email,otp,"Đặt lại mật khẩu ServletCRUDMVC","Mã OTP đặt lại mật khẩu của bạn: ");
    }
    private void send(String email,String otp,String subject,String text) {
        String username=AppConfig.get("mail.username","MAIL_USERNAME");
        String password=AppConfig.get("mail.password","MAIL_PASSWORD");
        String host=AppConfig.get("mail.host","MAIL_HOST");
        String port=AppConfig.get("mail.port","MAIL_PORT");
        if(username==null || username.isBlank() || password==null || password.isBlank()
                || host==null || host.isBlank() || port==null || port.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình SMTP. Vui lòng liên hệ quản trị viên rồi gửi lại OTP.");
        }
        Properties props=new Properties();
        props.setProperty("mail.smtp.host",host);
        props.setProperty("mail.smtp.port",port);
        props.setProperty("mail.smtp.auth","true");
        props.setProperty("mail.smtp.starttls.enable","true");
        props.setProperty("mail.smtp.starttls.required","true");
        props.setProperty("mail.smtp.ssl.checkserveridentity","true");
        props.setProperty("mail.smtp.connectiontimeout","10000");
        props.setProperty("mail.smtp.timeout","10000");
        props.setProperty("mail.smtp.writetimeout","10000");
        Session session=Session.getInstance(props);
        session.setDebug(false);
        try {
            MimeMessage message=new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipient(Message.RecipientType.TO,new InternetAddress(email,true));
            message.setSubject(subject,"UTF-8");
            message.setText(text+otp
                +"\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này với người khác.","UTF-8");
            try(Transport transport=session.getTransport("smtp")) {
                transport.connect(host,Integer.parseInt(port),username,password);
                transport.sendMessage(message,message.getAllRecipients());
            }
        } catch(Exception e) {

            throw new IllegalStateException(
                "Không gửi được email. Vui lòng thử gửi lại OTP sau 60 giây hoặc liên hệ quản trị viên."
            );
        }
    }
}
