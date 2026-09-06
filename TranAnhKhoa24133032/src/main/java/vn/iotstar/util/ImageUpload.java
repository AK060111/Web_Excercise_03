package vn.iotstar.util;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import jakarta.servlet.http.Part;
public final class ImageUpload {
    private ImageUpload(){}
    public static String save(Part part,String folder)throws IOException {
        if(!folder.equals("category")&&!folder.equals("product"))throw new IllegalArgumentException("Thư mục ảnh không hợp lệ");
        String filename=part.getSubmittedFileName();
        String name=filename==null?"":filename.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);
        int dot=name.lastIndexOf('.');String ext=dot<1?"":name.substring(dot+1).toLowerCase(Locale.ROOT);
        if(!ext.matches(folder.equals("category")?"png|jpe?g|gif|webp":"png|jpe?g|webp"))throw new IllegalArgumentException("Định dạng ảnh không hợp lệ. Sản phẩm nhận JPG, JPEG, PNG hoặc WEBP.");
        if(part.getSize()>5*1024*1024)throw new IllegalArgumentException("Ảnh tối đa 5 MB.");
        Path directory=Path.of(Constant.DIR,folder);Files.createDirectories(directory);
        String generated=System.currentTimeMillis()+"-"+UUID.randomUUID()+"."+ext;
        Path target=directory.resolve(generated);
        try(var input=part.getInputStream()){Files.copy(input,target);}
        catch(IOException e){Files.deleteIfExists(target);throw e;}
        return folder+"/"+generated;
    }
    public static void discardNewProductImage(String image){
        if(image==null||!image.matches("product/[0-9]+-[a-f0-9-]+\\.(jpg|jpeg|png|webp)"))return;
        try{Files.deleteIfExists(Path.of(Constant.DIR).resolve(image));}catch(IOException e){/* Retain unused file if unavailable. */}
    }
}
