package vn.iotstar.util;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import jakarta.servlet.http.Part;
public final class ImageUpload {
    private ImageUpload(){}
    public static String save(Part part,String folder)throws IOException {
        if(!folder.equals("category")&&!folder.equals("product")&&!folder.equals("avatar"))throw new IllegalArgumentException("Thư mục ảnh không hợp lệ");
        String filename=part.getSubmittedFileName();
        String name=filename==null?"":filename.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);
        int dot=name.lastIndexOf('.');String ext=dot<1?"":name.substring(dot+1).toLowerCase(Locale.ROOT);
        String allowed=folder.equals("avatar")?"png|jpe?g|gif":folder.equals("category")?"png|jpe?g|gif|webp":"png|jpe?g|webp";
        if(!ext.matches(allowed))throw new IllegalArgumentException("Định dạng ảnh không hợp lệ. Avatar nhận JPG, JPEG, PNG hoặc GIF; sản phẩm nhận JPG, JPEG, PNG hoặc WEBP.");
        if(part.getSize()>5*1024*1024)throw new IllegalArgumentException("Ảnh tối đa 5 MB.");
        validateImage(part,ext);
        Path root=Path.of(Constant.DIR);Files.createDirectories(root);root=root.toRealPath();
        Path directory=root.resolve(folder);Files.createDirectories(directory);directory=directory.toRealPath();
        if(!directory.startsWith(root))throw new IllegalArgumentException("Thư mục ảnh không hợp lệ.");
        String generated=System.currentTimeMillis()+"-"+UUID.randomUUID()+"."+ext;
        Path target=directory.resolve(generated);
        try(var input=part.getInputStream()){Files.copy(input,target);}
        catch(IOException e){Files.deleteIfExists(target);throw e;}
        return folder+"/"+generated;
    }
    public static void discardNewProductImage(String image){
        discardNewImage(image,"product");
    }
    public static void discardNewAvatar(String image){discardNewImage(image,"avatar");}
    public static void discardNewCategoryImage(String image){discardNewImage(image,"category");}
    private static void discardNewImage(String image,String folder){
        if(image==null||!image.matches(folder+"/[0-9]+-[a-f0-9-]+\\.(jpg|jpeg|png|webp|gif)"))return;
        try{Path root=Path.of(Constant.DIR).toRealPath();Path file=root.resolve(image).toRealPath();
            if(file.startsWith(root))Files.deleteIfExists(file);
        }catch(IOException e){/* Retain unused file if unavailable. */}
    }
    private static void validateImage(Part part,String ext)throws IOException {
        // Check image content, not only the client filename; limit decoded dimensions.
        try(var input=part.getInputStream();var imageInput=new javax.imageio.stream.MemoryCacheImageInputStream(input)){
            javax.imageio.ImageReader reader;
            if(ext.equals("webp")){
                // Explicit provider avoids relying on a container-wide ImageIO registry after redeploy.
                var provider=new com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi();
                if(!provider.canDecodeInput(imageInput))throw new IllegalArgumentException("File không phải ảnh WEBP hợp lệ.");
                reader=provider.createReaderInstance();
            }else{
                var readers=javax.imageio.ImageIO.getImageReaders(imageInput);
                if(!readers.hasNext())throw new IllegalArgumentException("File không phải ảnh được hỗ trợ.");
                reader=readers.next();
            }
            try{
                reader.setInput(imageInput);
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!(format.equals(ext)||(format.equals("jpeg")&&ext.equals("jpg"))))
                    throw new IllegalArgumentException("Nội dung ảnh không khớp phần mở rộng.");
                if(reader.getWidth(0)<1||reader.getHeight(0)<1||reader.getWidth(0)>4096||reader.getHeight(0)>4096)
                    throw new IllegalArgumentException("Ảnh tối đa 4096 x 4096 pixel.");
                if(reader.read(0)==null)throw new IllegalArgumentException("Không thể đọc nội dung ảnh.");
            }finally{reader.dispose();}
        }
    }
}
