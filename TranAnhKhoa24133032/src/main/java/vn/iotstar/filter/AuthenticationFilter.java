package vn.iotstar.filter;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.iotstar.util.Constant;

@WebFilter(urlPatterns = {"/admin/*", "/views/admin/*"})
public class AuthenticationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(Constant.SESSION_ACCOUNT) == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        String path=req.getServletPath();
        if((path.equals("/admin/product")||path.startsWith("/admin/product/"))
                && ((vn.iotstar.model.User)session.getAttribute(Constant.SESSION_ACCOUNT)).getRoleid()!=1){
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ quản trị viên được quản lý sản phẩm.");
            return;
        }
        chain.doFilter(request, response);
    }
}
