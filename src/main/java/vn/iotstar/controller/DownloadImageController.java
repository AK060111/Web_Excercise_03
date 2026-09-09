package vn.iotstar.controller;

import java.io.*;
import java.nio.file.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.util.Constant;

@WebServlet("/image")
public class DownloadImageController extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name=req.getParameter("fname");
        if(name==null||name.isBlank()||name.contains("..")||name.contains(":")||name.contains("\\")||name.startsWith("/")){
            resp.sendError(400);return;
        }
        try {
            Path relative=Path.of(name);
            if(relative.isAbsolute()){resp.sendError(400);return;}
            String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(java.util.Locale.ROOT);
            String type=switch(ext){case "jpg","jpeg"->"image/jpeg";case "png"->"image/png";case "gif"->"image/gif";case "webp"->"image/webp";default->null;};
            if(type==null){resp.sendError(400);return;}
            Path root=Path.of(Constant.DIR).toRealPath();
            Path candidate=root.resolve(relative).normalize();
            if(!candidate.startsWith(root)){resp.sendError(400);return;}
            Path file=candidate.toRealPath();
            if(!file.startsWith(root)){resp.sendError(400);return;}
            if(!Files.isRegularFile(file)){resp.sendError(404);return;}
            resp.setContentType(type);resp.setHeader("X-Content-Type-Options","nosniff");
            resp.setContentLengthLong(Files.size(file));Files.copy(file,resp.getOutputStream());
        }catch(InvalidPathException e){resp.sendError(400);}
        catch(NoSuchFileException e){resp.sendError(404);}
        catch(IOException e){if(!resp.isCommitted())resp.sendError(404);}
    }
}
