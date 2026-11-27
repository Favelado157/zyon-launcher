package dev.zyon;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;
/** Bounded image decode and OptiFine-compatible cape atlas sizing. */
public final class CapeImages {
 public static String nickname(String name){if(name==null||!name.matches("[A-Za-z0-9_]{3,16}"))throw new IllegalArgumentException("Nick invalido");return name;}
 public static BufferedImage decode(InputStream source)throws IOException{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
  while((n=source.read(buffer))!=-1){if(bytes.size()+n>1048576)throw new IOException("Cape image too large");bytes.write(buffer,0,n);}
  try(ImageInputStream in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes.toByteArray()))){if(in==null)throw new IOException("Invalid cape");Iterator<ImageReader> readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new IOException("Invalid cape format");ImageReader reader=readers.next();try{reader.setInput(in);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>1024||h>512)throw new IOException("Invalid cape dimensions");return atlas(reader.read(0));}finally{reader.dispose();}}
 }
 public static BufferedImage atlas(BufferedImage source){int w=64,h=32;while(w<source.getWidth()||h<source.getHeight()){w*=2;h*=2;}BufferedImage atlas=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=atlas.createGraphics();try{g.drawImage(source,0,0,null);}finally{g.dispose();}return atlas;}
}
