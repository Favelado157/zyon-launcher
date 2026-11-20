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
@Mod(modid="zyon",name="Zyon Client",version="1.3.1",acceptedMinecraftVersions="[1.8.9]",clientSideOnly=true)
public final class Zyon {
 public static Zyon instance;public final List<Feature> features=new ArrayList<Feature>();public Visuals visuals;public Profiles profiles;public Keystrokes keystrokes;public boolean requestMenu;
 public final KeyBinding menuKey=new KeyBinding("Zyon: abrir menu",Keyboard.KEY_RSHIFT,"Zyon"),panicKey=new KeyBinding("Zyon: ocultar extras",Keyboard.KEY_END,"Zyon");
 public final KeyBinding editorKey=new KeyBinding("Zyon: editar keystrokes",Keyboard.KEY_RCONTROL,"Zyon");
 public final KeyBinding zoomKey=new KeyBinding("Zyon: segurar zoom",Keyboard.KEY_C,"Zyon");
 private final Minecraft mc=Minecraft.getMinecraft();
 @Mod.EventHandler public void init(FMLInitializationEvent event){
  instance=this;registerFeatures();visuals=new Visuals(this);keystrokes=new Keystrokes(this);profiles=new Profiles(this,new java.io.File(mc.mcDataDir,"config/zyon"));
  ClientRegistry.registerKeyBinding(menuKey);ClientRegistry.registerKeyBinding(panicKey);ClientRegistry.registerKeyBinding(editorKey);ClientRegistry.registerKeyBinding(zoomKey);
  FMLCommonHandler.instance().bus().register(this);FMLCommonHandler.instance().bus().register(visuals);MinecraftForge.EVENT_BUS.register(visuals);MinecraftForge.EVENT_BUS.register(new Nameplates(this));MinecraftForge.EVENT_BUS.register(new Branding());Branding.applyWindowIcons();
  ClientCommandHandler.instance.registerCommand(new ZyonCommand(this));profiles.loadAutomatic();
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){profiles.flushOnExit();}},"Zyon-save"));
 }
 private void add(String id,String name,String desc,Feature.Category category,Setting... settings){features.add(new Feature(id,name,desc,category,settings));}
 private void registerFeatures(){
  add("crosshair","Mira","Personaliza a mira exibida na tela, em primeira pessoa.",RENDER,number("size","Tamanho",5,2,16,1),number("gap","Espaco central",3,1,12,1),bool("dot","Ponto central",true));
  add("zoom","Zoom","Aproxima a imagem enquanto a tecla de zoom estiver pressionada.",RENDER,number("factor","Ampliacao",3,1.5,6,.5));
  add("hud","HUD","Marca, FPS, coordenadas e recursos visuais ativos.",CLIENT,bool("logo","Mostrar icone",true),bool("fps","Mostrar FPS",true),bool("coords","Mostrar coordenadas",true),bool("modules","Mostrar recursos",true),number("x","Margem X",12,0,500,1),number("y","Margem Y",12,0,300,1));
  add("keystrokes","Keystrokes","Mostra teclas e cliques. Use Editar HUD para arrastar.",CLIENT,
   bool("mouse","Botoes do mouse",true),bool("cps","Cliques por segundo",true),bool("jump","Mostrar pulo",true),
   number("scale","Tamanho",1,.5,2,.1),choice("color","Cor ao pressionar",0,"Tema","Branco","Azul","Verde","Rosa"),number("opacity","Opacidade do fundo",190,0,255,1),
   number("x","Posicao horizontal (%)",2,0,100,.1),number("y","Posicao vertical (%)",35,0,100,.1));
  add("badge","Insignia Zyon","Mostra seu emblema antes do nick no TAB e no proprio nick em terceira pessoa. Visivel neste cliente; nao altera cargos ou prefixos do servidor.",CLIENT);
  add("selfname","Meu nick no F5","Mostra seu proprio nick acima da cabeca nas duas cameras do F5. Preserva as cores do servidor; oculta com F1, ao agachar ou ficar invisivel.",CLIENT,number("scale","Tamanho do nick",1,.7,1.5,.1));
  add("theme","Tema","Cores do menu e HUD. Aplicado diretamente.",INTERFACE,bool("menu","Menu inicial Zyon",true),choice("accent","Cor de destaque",0,"Azul neon","Azul","Ciano","Verde","Rosa","Laranja"),number("opacity","Opacidade",245,160,255,1),bool("rainbow","Cores animadas",false));
 }
 public Feature feature(String id){for(Feature f:features)if(f.id.equalsIgnoreCase(id)||f.name.equalsIgnoreCase(id))return f;throw new IllegalArgumentException("Modulo desconhecido: "+id);}
 public int accent(){Feature t=feature("theme");return t.flag("rainbow")?java.awt.Color.HSBtoRGB((System.currentTimeMillis()%8000)/8000f,.6f,1):new int[]{0xFF249BFF,0xFF72A7FF,0xFF58D9E8,0xFF72D9A0,0xFFFF83B7,0xFFFFB266}[t.setting("accent").integer()];}
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
