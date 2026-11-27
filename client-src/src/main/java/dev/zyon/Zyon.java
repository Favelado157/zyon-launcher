// Java / Zyon.java / Java 8 / Minecraft 1.8.9
package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import dev.zyon.platform.event.SubscribeEvent;
import dev.zyon.platform.event.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.util.*;
import static dev.zyon.Setting.*;
import static dev.zyon.Feature.Category.*;
public final class Zyon {
 public static Zyon instance;public final List<Feature> features=new ArrayList<Feature>();public Engine engine;public Visuals visuals;public Profiles profiles;public Keystrokes keystrokes;public boolean requestMenu;public AutoTextStore autoText;public AutoTextHotkeys autoTextHotkeys;
 public final KeyBinding menuKey=new KeyBinding("key.zyon.menu",Keyboard.KEY_RSHIFT,"key.categories.zyon"),panicKey=new KeyBinding("key.zyon.hide",Keyboard.KEY_END,"key.categories.zyon");
 public final KeyBinding editorKey=new KeyBinding("key.zyon.editor",Keyboard.KEY_RCONTROL,"key.categories.zyon");
 public final KeyBinding zoomKey=new KeyBinding("key.zyon.zoom",Keyboard.KEY_C,"key.categories.zyon");
 private final Minecraft mc=Minecraft.getMinecraft();
 public void init(){
  instance=this;registerFeatures();engine=new Engine(this);dev.zyon.platform.Platform.EVENTS.register(engine);visuals=new Visuals(this);keystrokes=new Keystrokes(this);profiles=new Profiles(this,new java.io.File(mc.mcDataDir,"config/zyon"));
  autoText=new AutoTextStore(new java.io.File(mc.mcDataDir,"config/zyon/chat/autotext.json"));try{autoText.load();}catch(java.io.IOException e){org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Autotexto nao foi carregado; arquivo original preservado.");}
  autoTextHotkeys=new AutoTextHotkeys(this);dev.zyon.platform.Platform.EVENTS.register(autoTextHotkeys);
  dev.zyon.platform.Platform.key(menuKey);dev.zyon.platform.Platform.key(panicKey);dev.zyon.platform.Platform.key(editorKey);dev.zyon.platform.Platform.key(zoomKey);
  dev.zyon.platform.Platform.EVENTS.register(this);dev.zyon.platform.Platform.EVENTS.register(visuals);dev.zyon.platform.Platform.EVENTS.register(visuals);dev.zyon.platform.Platform.EVENTS.register(new Nameplates(this));dev.zyon.platform.Platform.EVENTS.register(new PlayerHealth(this));dev.zyon.platform.Platform.EVENTS.register(new ChatAutoText(this));dev.zyon.platform.Platform.EVENTS.register(new LocalEffects(this));dev.zyon.platform.Platform.EVENTS.register(new LocalEffects.Brightness(this));dev.zyon.platform.Platform.EVENTS.register(new ResourcePackFolders());dev.zyon.platform.Platform.EVENTS.register(new Branding());Branding.applyWindowIcons();
  dev.zyon.platform.Platform.command=new ZyonCommand(this);mc.gameSettings.loadOptions();profiles.loadAutomatic();
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){profiles.flushOnExit();}},"Zyon-save"));
 }
 private void add(String id,String name,String desc,Feature.Category category,Setting... settings){features.add(new Feature(id,name,desc,category,settings));}
 private void registerFeatures(){Catalog.register(features);}
 public Feature feature(String id){for(Feature f:features)if(f.id.equalsIgnoreCase(id)||f.name.equalsIgnoreCase(id))return f;throw new IllegalArgumentException("Modulo desconhecido: "+id);}
 public int accent(){Feature t=feature("theme");if(t.flag("customAccent"))return t.setting("highlight").rgb();return t.flag("rainbow")?java.awt.Color.HSBtoRGB((System.currentTimeMillis()%8000)/8000f,.6f,1):new int[]{0xFF249BFF,0xFF72A7FF,0xFF58D9E8,0xFF72D9A0,0xFFFF83B7,0xFFFFB266}[t.setting("accent").integer()];}
 @SubscribeEvent public void key(InputEvent.KeyInputEvent event){
  int key=Keyboard.getEventKey();if(autoTextHotkeys.handle(key,Keyboard.getEventKeyState(),Keyboard.isRepeatEvent()))return;
  if(!Keyboard.getEventKeyState()||Keyboard.isRepeatEvent()||mc.currentScreen!=null)return;
  if(key==menuKey.getKeyCode()){mc.displayGuiScreen(new ClickGui(this));return;}if(key==panicKey.getKeyCode()){panic();return;}
  if(key==editorKey.getKeyCode()){mc.displayGuiScreen(new HudEditor(this,null));return;}
  if(key==zoomKey.getKeyCode())return;
  for(Feature f:features)if(f.key>0&&f.key==key)f.toggle();profiles.saveAutomatic();
 }
 @SubscribeEvent public void mouse(InputEvent.MouseInputEvent event){if(mc.thePlayer!=null&&mc.currentScreen==null&&Mouse.getEventButtonState())keystrokes.click(Mouse.getEventButton());}
 public void panic(){for(Feature f:features)f.setEnabled(false);profiles.saveAutomatic();message("Todos os recursos desligados.");}
 public void message(String text){if(mc.thePlayer!=null)mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b[Zyon] \u00a7f"+text));}
}
