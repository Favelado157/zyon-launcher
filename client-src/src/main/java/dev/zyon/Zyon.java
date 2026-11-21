// Java / Zyon.java / Java 8 / Minecraft Forge 1.8.9
package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.util.*;
import static dev.zyon.Setting.*;
import static dev.zyon.Feature.Category.*;
@Mod(modid="zyon",name="Zyon Client",version="1.5.2",acceptedMinecraftVersions="[1.8.9]",clientSideOnly=true)
public final class Zyon {
 public static Zyon instance;public final List<Feature> features=new ArrayList<Feature>();public Visuals visuals;public Profiles profiles;public Keystrokes keystrokes;public boolean requestMenu;public AutoTextStore autoText;
 public final KeyBinding menuKey=new KeyBinding("Zyon: abrir menu",Keyboard.KEY_RSHIFT,"Zyon"),panicKey=new KeyBinding("Zyon: ocultar extras",Keyboard.KEY_END,"Zyon");
 public final KeyBinding editorKey=new KeyBinding("Zyon: editar keystrokes",Keyboard.KEY_RCONTROL,"Zyon");
 public final KeyBinding zoomKey=new KeyBinding("Zyon: segurar zoom",Keyboard.KEY_C,"Zyon");
 private final Minecraft mc=Minecraft.getMinecraft();
 @Mod.EventHandler public void init(FMLInitializationEvent event){
  instance=this;registerFeatures();visuals=new Visuals(this);keystrokes=new Keystrokes(this);profiles=new Profiles(this,new java.io.File(mc.mcDataDir,"config/zyon"));
  autoText=new AutoTextStore(new java.io.File(mc.mcDataDir,"config/zyon/chat/autotext.json"));try{autoText.load();}catch(java.io.IOException e){org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Autotexto nao foi carregado; arquivo original preservado.");}
  ClientRegistry.registerKeyBinding(menuKey);ClientRegistry.registerKeyBinding(panicKey);ClientRegistry.registerKeyBinding(editorKey);ClientRegistry.registerKeyBinding(zoomKey);
  FMLCommonHandler.instance().bus().register(this);FMLCommonHandler.instance().bus().register(visuals);MinecraftForge.EVENT_BUS.register(visuals);MinecraftForge.EVENT_BUS.register(new Nameplates(this));MinecraftForge.EVENT_BUS.register(new PlayerHealth(this));MinecraftForge.EVENT_BUS.register(new ChatAutoText(this));MinecraftForge.EVENT_BUS.register(new LocalEffects(this));FMLCommonHandler.instance().bus().register(new LocalEffects.Brightness(this));MinecraftForge.EVENT_BUS.register(new ResourcePackFolders());MinecraftForge.EVENT_BUS.register(new Branding());Branding.applyWindowIcons();
  ClientCommandHandler.instance.registerCommand(new ZyonCommand(this));profiles.loadAutomatic();
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){profiles.flushOnExit();}},"Zyon-save"));
 }
 private void add(String id,String name,String desc,Feature.Category category,Setting... settings){features.add(new Feature(id,name,desc,category,settings));}
 private void registerFeatures(){Catalog.register(features);}
 public Feature feature(String id){for(Feature f:features)if(f.id.equalsIgnoreCase(id)||f.name.equalsIgnoreCase(id))return f;throw new IllegalArgumentException("Modulo desconhecido: "+id);}
 public int accent(){Feature t=feature("theme");if(t.flag("customAccent"))return t.setting("highlight").rgb();return t.flag("rainbow")?java.awt.Color.HSBtoRGB((System.currentTimeMillis()%8000)/8000f,.6f,1):new int[]{0xFF249BFF,0xFF72A7FF,0xFF58D9E8,0xFF72D9A0,0xFFFF83B7,0xFFFFB266}[t.setting("accent").integer()];}
 @SubscribeEvent public void key(InputEvent.KeyInputEvent event){
  if(!Keyboard.getEventKeyState()||Keyboard.isRepeatEvent()||mc.currentScreen!=null)return;int key=Keyboard.getEventKey();
  if(key==menuKey.getKeyCode()){mc.displayGuiScreen(new ClickGui(this));return;}if(key==panicKey.getKeyCode()){panic();return;}
  if(key==editorKey.getKeyCode()){mc.displayGuiScreen(new HudEditor(this,null));return;}
  if(key==zoomKey.getKeyCode())return;
  for(Feature f:features)if(f.key>0&&f.key==key)f.toggle();profiles.saveAutomatic();
 }
 @SubscribeEvent public void mouse(InputEvent.MouseInputEvent event){if(mc.thePlayer!=null&&mc.currentScreen==null&&Mouse.getEventButtonState())keystrokes.click(Mouse.getEventButton());}
 public void panic(){for(Feature f:features)f.setEnabled(false);profiles.saveAutomatic();message("Recursos visuais ocultados.");}
 public void message(String text){if(mc.thePlayer!=null)mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b[Zyon] \u00a7f"+text));}
}
