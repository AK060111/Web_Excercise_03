package vn.iotstar.util;
import jakarta.mail.internet.InternetAddress;

/** Shared form rules; passwords are never trimmed or transformed. */
public final class ValidationUtil {
    private ValidationUtil() { }
    public static String fullname(String value) {
        String name = java.text.Normalizer.normalize(value == null ? "" : value,
                java.text.Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").trim();
        name = required(name, "Họ và tên", 255);
        if (!name.matches("[\\p{L} ]+"))
            throw new IllegalArgumentException("Họ và tên chỉ được chứa chữ cái và khoảng trắng.");
        return name;
    }
    public static String trim(String value) { return value == null ? "" : value.trim(); }
    public static String required(String value, String label, int max) {
        String result = trim(value);
        if (result.isEmpty()) throw new IllegalArgumentException(label + " không được rỗng.");
        if (result.length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
        return result;
    }
    public static String email(String value) {
        String email = required(value, "Email", 255);
        try {
            InternetAddress address = new InternetAddress(email, true); address.validate();
            if (!email.equals(address.getAddress()) || !email.contains("@")) throw new IllegalArgumentException();
        } catch (Exception e) { throw new IllegalArgumentException("Email không hợp lệ."); }
        return email;
    }
    public static void password(String value) {
        if (value == null || value.isBlank() || value.length() < 8 || value.length() > 255)
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 255 ký tự.");
    }
    public static String phone(String value) {
        String phone = trim(value);
        if (phone.isEmpty()) return null;
        if (!phone.matches("(?:0|\\+84)[35789][0-9]{8}"))
            throw new IllegalArgumentException("Số di động Việt Nam phải gồm 10 số bắt đầu bằng 0, hoặc dạng +84 thay cho số 0 đầu.");
        return phone;
    }
    public static boolean otp(String value) { return value != null && value.matches("[0-9]{6}"); }
}
