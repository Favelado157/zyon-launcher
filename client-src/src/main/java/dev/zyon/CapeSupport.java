package dev.zyon;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
/** Loads public OptiFine capes by nickname without OptiFine classes or Forge. */
public final class CapeSupport {
 private static final class Entry {volatile ResourceLocation texture;volatile long retry;volatile boolean loading;}
 private static final ConcurrentMap<String,Entry> entries=new ConcurrentHashMap<String,Entry>();
 private static final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(32),new ThreadFactory(){public Thread newThread(Runnable r){Thread t=new Thread(r,"Zyon-cape");t.setDaemon(true);return t;}},new ThreadPoolExecutor.AbortPolicy());
 public static ResourceLocation location(ResourceLocation original,AbstractClientPlayer player){
  Feature feature=Zyon.instance.feature("optifinecape");Minecraft mc=Minecraft.getMinecraft();if(!feature.enabled()||(!feature.flag("others")&&player!=mc.thePlayer))return original;
  final String name;try{name=CapeImages.nickname(player.getName());}catch(IllegalArgumentException e){return original;}
  if(entries.size()>=256&&!entries.containsKey(name))return original;
  Entry entry=entries.get(name);if(entry==null){Entry made=new Entry(),old=entries.putIfAbsent(name,made);entry=old==null?made:old;}
  if(!entry.loading&&entry.texture==null&&System.currentTimeMillis()>=entry.retry){final Entry pending=entry;pending.loading=true;try{worker.execute(new Runnable(){public void run(){load(name,pending);}});}catch(RejectedExecutionException e){pending.loading=false;pending.retry=System.currentTimeMillis()+30000;}}
  return entry.texture==null?original:entry.texture;
 }
 private static void load(final String name,final Entry entry){
  Minecraft mc=Minecraft.getMinecraft();Path cache=new File(mc.mcDataDir,"config/zyon/capes/"+name+".png").toPath();
  try{
   BufferedImage image=null;
   if(Files.isRegularFile(cache)&&Files.size(cache)<=1048576&&System.currentTimeMillis()-Files.getLastModifiedTime(cache).toMillis()<86400000)try(InputStream in=Files.newInputStream(cache)){image=CapeImages.decode(in);}catch(IOException ignored){}
   if(image==null){image=CapeDownload.fetch(name);
    try{Files.createDirectories(cache.getParent());Path temp=Files.createTempFile(cache.getParent(),"cape-",".png");try{ImageIOBridge.write(image,temp);Files.move(temp,cache,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}}catch(IOException ignored){}
   }
   final BufferedImage ready=image;mc.addScheduledTask(new Runnable(){public void run(){try{entry.texture=Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("zyon-optifine-"+name,new DynamicTexture(ready));}finally{entry.retry=System.currentTimeMillis()+600000;entry.loading=false;}}});
  }catch(Exception e){entry.retry=System.currentTimeMillis()+600000;entry.loading=false;}
 }
 private static final class ImageIOBridge {static void write(BufferedImage image,Path file)throws IOException{javax.imageio.ImageIO.write(image,"PNG",file.toFile());}}
}
