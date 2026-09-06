package vn.iotstar.controller;

import java.io.*;
import java.nio.file.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.util.Constant;

@WebServlet("/image")
public class DownloadImageController extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name=req.getParameter("fname"); if(name==null || name.contains("..") || new File(name).isAbsolute()){resp.sendError(400);return;}
        File file=new File(Constant.DIR,name); if(!file.isFile()){resp.sendError(404);return;}
        String type=getServletContext().getMimeType(file.getName()); resp.setContentType(type==null?"image/jpeg":type); resp.setContentLengthLong(file.length()); Files.copy(file.toPath(),resp.getOutputStream());
    }
}
