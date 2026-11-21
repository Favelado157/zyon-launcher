package dev.zyon;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Local saved messages. Loading and editing never send a message. */
public final class AutoTextStore {
 public static final int SLOTS=6;
 private final File file;private final int[] keys=new int[SLOTS];
 private final String[] names=new String[SLOTS],messages=new String[SLOTS];
 public AutoTextStore(File file){this.file=file;for(int i=0;i<SLOTS;i++){names[i]="Atalho "+(i+1);messages[i]="";}}
 public int key(int i){return keys[i];}
 public void setKey(int i,int key){if(key<0||key>255)throw new IllegalArgumentException("Tecla invalida.");for(int j=0;j<SLOTS;j++)if(j!=i&&key!=0&&keys[j]==key)throw new IllegalArgumentException("Essa tecla ja esta em outra mensagem.");keys[i]=key;}
 public void setMessage(int i,String message){messages[i]=clean(message,100);}
 public String name(int i){return names[i];}public String message(int i){return messages[i];}
 public void set(int i,String name,String message){names[i]=clean(name,16);messages[i]=clean(message,100);}
 public static String clean(String text,int limit){StringBuilder out=new StringBuilder();if(text!=null)for(int i=0;i<text.length()&&out.length()<limit;i++){char c=text.charAt(i);if(c>=32&&c!=127&&c!='\u00a7')out.append(c);}if(out.length()>0&&Character.isHighSurrogate(out.charAt(out.length()-1)))out.setLength(out.length()-1);return out.toString();}
 public void load()throws IOException{
  if(!file.isFile())return;if(file.length()>65536)throw new IOException("Arquivo de autotexto excede o limite.");
  String[] nextNames=new String[SLOTS],nextMessages=new String[SLOTS];int[] nextKeys=new int[SLOTS];java.util.Set<Integer> used=new java.util.HashSet<Integer>();
  try(Reader in=new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)){
   JsonObject root=new JsonParser().parse(in).getAsJsonObject();int schema=root.get("schema").getAsInt();if(schema!=1&&schema!=2)throw new IllegalArgumentException("Versao desconhecida.");JsonArray slots=root.getAsJsonArray("slots");if(slots==null||slots.size()>SLOTS)throw new IllegalArgumentException("Atalhos invalidos.");
   for(int i=0;i<slots.size();i++){JsonObject slot=slots.get(i).getAsJsonObject();nextNames[i]=slot.has("name")?clean(slot.get("name").getAsString(),16):"Atalho "+(i+1);if(schema==2&&slot.has("key")){double key=slot.get("key").getAsDouble();if(!Double.isFinite(key)||key!=Math.floor(key)||key<0||key>255)throw new IllegalArgumentException("Tecla invalida.");nextKeys[i]=(int)key;if(key!=0&&!used.add((int)key))throw new IllegalArgumentException("Tecla duplicada.");}nextMessages[i]=clean(slot.get("message").getAsString(),100);}
  }catch(RuntimeException e){throw new IOException("Autotexto invalido.",e);}
  for(int i=0;i<SLOTS;i++)if(nextNames[i]!=null){names[i]=nextNames[i];messages[i]=nextMessages[i];keys[i]=nextKeys[i];}
 }
 public void save()throws IOException{
  JsonObject root=new JsonObject();root.addProperty("schema",2);JsonArray slots=new JsonArray();for(int i=0;i<SLOTS;i++){JsonObject slot=new JsonObject();slot.addProperty("name",names[i]);slot.addProperty("key",keys[i]);slot.addProperty("message",messages[i]);slots.add(slot);}root.add("slots",slots);
  Path parent=file.toPath().getParent();Files.createDirectories(parent);Path temp=Files.createTempFile(parent,".autotext-",".tmp");try{Files.write(temp,new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8));try{Files.move(temp,file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
 }
}
