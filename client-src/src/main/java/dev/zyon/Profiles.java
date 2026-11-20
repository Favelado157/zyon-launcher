// Java / Profiles.java / Java 8 / Forge 1.8.9
package dev.zyon;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;
public final class Profiles {
 private final Zyon client;private final File directory;private final Gson gson=new GsonBuilder().setPrettyPrinting().create();private volatile String lastSnapshot;
 public Profiles(Zyon client,File directory){this.client=client;this.directory=directory;}
 private File file(String name){if(name==null||!name.matches("[a-zA-Z0-9_-]{1,32}"))throw new IllegalArgumentException("Use 1 a 32 letras, numeros, _ ou - no nome.");return new File(directory,name.toLowerCase(Locale.ROOT)+".json");}
 public void loadAutomatic(){client.feature("zoom").setEnabled(true);client.feature("keystrokes").setEnabled(true);if(!new File(directory,"auto.json").isFile()){client.feature("hud").setEnabled(true);saveAutomatic();return;}try{load("auto");}catch(Exception e){LogManager.getLogger("Zyon").error("Erro ao carregar auto.json",e);}}
 private String snapshot(){JsonObject root=new JsonObject(),modules=new JsonObject();root.addProperty("schema",1);for(Feature f:client.features){JsonObject obj=new JsonObject(),settings=new JsonObject();obj.addProperty("enabled",f.enabled());obj.addProperty("key",f.key);for(Setting s:f.settings)settings.addProperty(s.id,s.get());obj.add("settings",settings);modules.add(f.id,obj);}root.add("modules",modules);return gson.toJson(root);}
 private synchronized void write(File file,String text)throws IOException{Files.createDirectories(directory.toPath());Path tmp=Files.createTempFile(directory.toPath(),".zyon-",".tmp");try{Files.write(tmp,text.getBytes(StandardCharsets.UTF_8));try{Files.move(tmp,file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}}
 public void save(String name)throws IOException{File target=file(name);String json=snapshot();write(target,json);if(name.equalsIgnoreCase("auto"))lastSnapshot=json;}
 public void saveAutomatic(){try{save("auto");}catch(Exception e){LogManager.getLogger("Zyon").error("Erro ao salvar perfil",e);client.message("Falha ao salvar; consulte latest.log.");}}
 public void flushOnExit(){String text=lastSnapshot;if(text!=null)try{write(file("auto"),text);}catch(IOException e){LogManager.getLogger("Zyon").error("Erro ao sair",e);}}
 public void load(String name)throws IOException{
  File source=file(name);if(!source.isFile())throw new IOException("Perfil nao encontrado: "+name);if(source.length()>1048576)throw new IOException("Perfil excede 1 MB.");
  Map<Feature,Boolean> states=new LinkedHashMap<Feature,Boolean>();Map<Feature,Integer> keys=new LinkedHashMap<Feature,Integer>();Map<Setting,Double> values=new LinkedHashMap<Setting,Double>();
  try(Reader reader=new InputStreamReader(new FileInputStream(source),StandardCharsets.UTF_8)){
   JsonObject root=new JsonParser().parse(reader).getAsJsonObject();if(!root.has("schema")||root.get("schema").getAsInt()!=1)throw new IllegalArgumentException("Versao de perfil desconhecida.");JsonObject modules=root.getAsJsonObject("modules");if(modules==null)throw new IllegalArgumentException("Perfil sem modulos.");
   for(Feature f:client.features){if(!modules.has(f.id))continue;JsonObject obj=modules.getAsJsonObject(f.id);if(obj.has("enabled"))states.put(f,obj.get("enabled").getAsBoolean());if(obj.has("key")){int key=obj.get("key").getAsInt();if(key<0||key>=org.lwjgl.input.Keyboard.KEYBOARD_SIZE)throw new IllegalArgumentException("Atalho invalido.");keys.put(f,key);}JsonObject settings=obj.getAsJsonObject("settings");if(settings!=null)for(Setting s:f.settings)if(settings.has(s.id)){double value=settings.get(s.id).getAsDouble();if(Double.isNaN(value)||Double.isInfinite(value))throw new IllegalArgumentException("Valor nao finito.");values.put(s,value);}}
  }catch(RuntimeException e){throw new IOException("Perfil invalido: "+e.getMessage(),e);}
  for(Feature f:states.keySet())f.setEnabled(false);for(Map.Entry<Setting,Double> v:values.entrySet())v.getKey().set(v.getValue());for(Map.Entry<Feature,Integer> k:keys.entrySet())k.getKey().key=k.getValue();for(Map.Entry<Feature,Boolean> s:states.entrySet())s.getKey().setEnabled(s.getValue());lastSnapshot=snapshot();
 }
 public List<String> list(){List<String> names=new ArrayList<String>();File[] files=directory.listFiles();if(files!=null)for(File f:files)if(f.isFile()&&f.getName().endsWith(".json"))names.add(f.getName().substring(0,f.getName().length()-5));Collections.sort(names);return names;}
}
