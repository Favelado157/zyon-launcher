package dev.zyon;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Local saved messages. Loading and editing never send a message. */
public final class AutoTextStore {
 public static final int SLOTS=6;
 private final File file;
 private final String[] names=new String[SLOTS],messages=new String[SLOTS];
 public AutoTextStore(File file){this.file=file;for(int i=0;i<SLOTS;i++){names[i]="Atalho "+(i+1);messages[i]="";}}
 public String name(int i){return names[i];}public String message(int i){return messages[i];}
 public void set(int i,String name,String message){names[i]=clean(name,16);messages[i]=clean(message,100);}
 public static String clean(String text,int limit){StringBuilder out=new StringBuilder();if(text!=null)for(int i=0;i<text.length()&&out.length()<limit;i++){char c=text.charAt(i);if(c>=32&&c!=127&&c!='\u00a7')out.append(c);}if(out.length()>0&&Character.isHighSurrogate(out.charAt(out.length()-1)))out.setLength(out.length()-1);return out.toString();}
 public void load()throws IOException{
  if(!file.isFile())return;if(file.length()>65536)throw new IOException("Arquivo de autotexto excede o limite.");
  String[] nextNames=new String[SLOTS],nextMessages=new String[SLOTS];
  try(Reader in=new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)){
   JsonObject root=new JsonParser().parse(in).getAsJsonObject();if(root.get("schema").getAsInt()!=1)throw new IllegalArgumentException("Versao desconhecida.");JsonArray slots=root.getAsJsonArray("slots");if(slots==null||slots.size()>SLOTS)throw new IllegalArgumentException("Atalhos invalidos.");
   for(int i=0;i<slots.size();i++){JsonObject slot=slots.get(i).getAsJsonObject();nextNames[i]=clean(slot.get("name").getAsString(),16);nextMessages[i]=clean(slot.get("message").getAsString(),100);}
  }catch(RuntimeException e){throw new IOException("Autotexto invalido.",e);}
  for(int i=0;i<SLOTS;i++)if(nextNames[i]!=null){names[i]=nextNames[i];messages[i]=nextMessages[i];}
 }
 public void save()throws IOException{
  JsonObject root=new JsonObject();root.addProperty("schema",1);JsonArray slots=new JsonArray();for(int i=0;i<SLOTS;i++){JsonObject slot=new JsonObject();slot.addProperty("name",names[i]);slot.addProperty("message",messages[i]);slots.add(slot);}root.add("slots",slots);
  Path parent=file.toPath().getParent();Files.createDirectories(parent);Path temp=Files.createTempFile(parent,".autotext-",".tmp");try{Files.write(temp,new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8));try{Files.move(temp,file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
 }
}
