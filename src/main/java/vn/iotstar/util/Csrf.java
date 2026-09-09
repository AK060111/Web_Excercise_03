package vn.iotstar.util;
import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.http.*;
public final class Csrf {
    private Csrf() { }
    public static void prepare(HttpServletRequest req, String key) {
        HttpSession session = req.getSession();
        if (session.getAttribute(key) == null) session.setAttribute(key, UUID.randomUUID().toString());
    }
    public static boolean require(HttpServletRequest req, HttpServletResponse resp, String key) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(key) instanceof String token && token.equals(req.getParameter("csrf"))) return true;
        resp.sendError(403, "Phiên làm việc không hợp lệ. Vui lòng mở lại biểu mẫu.");
        return false;
    }
}
