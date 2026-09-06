package vn.iotstar.controller;
import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.util.Constant;

@WebServlet("/logout")
public class LogoutController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();

        // Match the cookie path used by LoginController to prevent automatic login again.
        Cookie remember = new Cookie(Constant.COOKIE_REMEMBER, "");
        remember.setMaxAge(0);
        remember.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        resp.addCookie(remember);
        resp.setHeader("Cache-Control", "no-store");
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
