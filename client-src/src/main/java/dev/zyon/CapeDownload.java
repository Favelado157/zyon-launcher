package dev.zyon;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
/** OptiFine's public image endpoint has a legacy HTTP compatibility route. No credentials are sent. */
public final class CapeDownload {
 public interface Source {InputStream open(String url)throws IOException;}
 public static BufferedImage fetch(String name,Source source)throws IOException{
  CapeImages.nickname(name);IOException last=null;
  for(String prefix:new String[]{"https://s.optifine.net/capes/","http://s.optifine.net/capes/"})try(InputStream in=source.open(prefix+name+".png")){return CapeImages.decode(in);}catch(IOException e){last=e;}
  throw last==null?new IOException("No cape available"):last;
 }
 public static BufferedImage fetch(String name)throws IOException{return fetch(name,new Source(){public InputStream open(String url)throws IOException{
  final HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","ZyonClient/2.0");
  try{if(c.getResponseCode()!=200)throw new IOException("No public cape for nickname");if(c.getContentLengthLong()>1048576)throw new IOException("Cape too large");return new FilterInputStream(c.getInputStream()){@Override public void close()throws IOException{try{super.close();}finally{c.disconnect();}}};}catch(IOException e){c.disconnect();throw e;}
 }});}
}
