package vn.iotstar.controller;
import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.model.User;
import vn.iotstar.util.Constant;
@WebServlet("/waiting") public class WaitingController extends HttpServlet { protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException {HttpSession s=req.getSession(false);if(s==null||s.getAttribute(Constant.SESSION_ACCOUNT)==null){resp.sendRedirect(req.getContextPath()+"/login");return;}User u=(User)s.getAttribute(Constant.SESSION_ACCOUNT);resp.sendRedirect(req.getContextPath()+(u.getRoleid()==1?"/admin/category/list":"/home"));} }
