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
@Mod(modid="zyon",name="Zyon Client",version="1.2.2",acceptedMinecraftVersions="[1.8.9]",clientSideOnly=true)
public final class Zyon {
 public static Zyon instance;public final List<Feature> features=new ArrayList<Feature>();public Engine engine;public Profiles profiles;public Keystrokes keystrokes;public boolean requestMenu;
 public final KeyBinding menuKey=new KeyBinding("Zyon: abrir menu",Keyboard.KEY_RSHIFT,"Zyon"),panicKey=new KeyBinding("Zyon: desligar tudo",Keyboard.KEY_END,"Zyon");
 public final KeyBinding editorKey=new KeyBinding("Zyon: editar keystrokes",Keyboard.KEY_RCONTROL,"Zyon");
 private final Minecraft mc=Minecraft.getMinecraft();
 @Mod.EventHandler public void init(FMLInitializationEvent event){
  instance=this;registerFeatures();engine=new Engine(this);keystrokes=new Keystrokes(this);profiles=new Profiles(this,new java.io.File(mc.mcDataDir,"config/zyon"));
  ClientRegistry.registerKeyBinding(menuKey);ClientRegistry.registerKeyBinding(panicKey);ClientRegistry.registerKeyBinding(editorKey);
  FMLCommonHandler.instance().bus().register(this);FMLCommonHandler.instance().bus().register(engine);MinecraftForge.EVENT_BUS.register(engine);MinecraftForge.EVENT_BUS.register(new Branding());Branding.applyWindowIcons();
  ClientCommandHandler.instance.registerCommand(new ZyonCommand(this));profiles.loadAutomatic();
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){profiles.flushOnExit();}},"Zyon-save"));
 }
 private void add(String id,String name,String desc,Feature.Category category,Setting... settings){features.add(new Feature(id,name,desc,category,settings));}
 private void registerFeatures(){
  add("killaura","KillAura","Ataca um alvo por intervalo dentro do alcance e FOV.",COMBAT,
   number("range","Alcance",3,1,6,.1),number("cps","Ataques / segundo",8,1,20,1),number("fov","Campo de visao",120,10,360,5),
   bool("players","Jogadores",true),bool("mobs","Monstros",false),bool("animals","Animais",false),bool("invisible","Invisiveis",false),
   bool("walls","Atraves de paredes",false),bool("rotate","Virar para o alvo",true),bool("weapon","Exigir espada ou machado",true),choice("sort","Prioridade",0,"Distancia","Vida"));
  add("triggerbot","TriggerBot","Ataca a entidade na mira. Usa os filtros do KillAura.",COMBAT,number("cps","Ataques / segundo",8,1,20,1),number("range","Alcance",3,1,6,.1),bool("weapon","Exigir arma",true));
  add("autoclicker","AutoClicker","Repete ataques ao segurar o botao esquerdo, sem minerar.",COMBAT,number("cps","Cliques / segundo",10,1,20,1),bool("weapon","Exigir arma",true));
  add("aimassist","AimAssist","Ajusta a mira ao segurar o botao esquerdo.",COMBAT,number("range","Alcance",4,1,6,.1),number("fov","Campo de visao",60,5,180,5),number("turn","Graus / tick",4,.5,20,.5),bool("weapon","Exigir arma",true));
  add("autosprint","AutoSprint","Mantem a corrida ao andar.",MOVEMENT,bool("omni","Todas as direcoes",false));
  add("fly","Fly","Espaco sobe, agachar desce.",MOVEMENT,number("speed","Velocidade horizontal",.5,.1,2,.05),number("vertical","Velocidade vertical",.35,.1,1.5,.05));
  add("speed","Speed","Velocidade horizontal e bunny hop opcional.",MOVEMENT,number("speed","Blocos / tick",.35,.1,1.2,.01),bool("bhop","Bunny hop",false));
  add("step","Step","Altura de subida; restaura ao desligar.",MOVEMENT,number("height","Altura em blocos",1,.6,2.5,.1));
  add("autojump","AutoJump","Pula automaticamente ao andar no chao.",MOVEMENT);
  add("safewalk","SafeWalk","Agacha perto de bordas sem apoio.",MOVEMENT,number("lookahead","Distancia da borda",.3,.1,.8,.05));
  add("fullbright","Fullbright","Aumenta o brilho e restaura o valor ao desligar.",RENDER,number("gamma","Intensidade",12,1,20,1));
  add("esp","ESP","Caixas em entidades carregadas no mundo.",RENDER,bool("players","Jogadores",true),bool("mobs","Monstros",false),bool("animals","Animais",false),bool("walls","Atraves de paredes",true),number("distance","Distancia maxima",64,8,160,1),number("width","Espessura",1.5,1,4,.5));
  add("tracers","Tracers","Linhas da camera ate entidades carregadas.",RENDER,bool("players","Jogadores",true),bool("mobs","Monstros",false),bool("animals","Animais",false),bool("walls","Atraves de paredes",true),number("distance","Distancia maxima",64,8,160,1));
  add("crosshair","Crosshair","Mira colorida sobre a mira padrao.",RENDER,number("size","Tamanho",5,2,16,1),number("gap","Espaco central",3,1,12,1),bool("dot","Ponto central",true));
  add("autotool","AutoTool","Seleciona a ferramenta mais rapida da hotbar.",PLAYER,bool("restore","Restaurar slot ao soltar",true));
  add("autoeat","AutoEat","Come alimentos da hotbar abaixo do limite de fome.",PLAYER,number("hunger","Limite de fome",14,1,19,1));
  add("fastplace","FastPlace","Reduz o intervalo local de colocacao de blocos.",PLAYER,number("delay","Intervalo em ticks",0,0,4,1));
  add("hud","HUD","Marca, FPS, coordenadas, alvo e modulos ativos.",CLIENT,bool("logo","Mostrar icone",true),bool("fps","Mostrar FPS",true),bool("coords","Mostrar coordenadas",true),bool("modules","Mostrar modulos",true),bool("target","Mostrar alvo",true),number("x","Margem X",12,0,500,1),number("y","Margem Y",12,0,300,1));
  add("theme","Tema","Cores do menu e HUD. Aplicado diretamente.",CLIENT,bool("menu","Menu inicial Zyon",true),choice("accent","Cor de destaque",0,"Azul neon","Azul","Ciano","Verde","Rosa","Laranja"),number("opacity","Opacidade",245,160,255,1),bool("rainbow","Cores animadas",false));
  add("keystrokes","Keystrokes","Teclas e cliques na tela. Use Editar HUD para arrastar.",CLIENT,
   bool("mouse","Botoes do mouse",true),bool("cps","Cliques por segundo",true),bool("jump","Mostrar pulo",true),
   number("scale","Tamanho",1,.5,2,.1),choice("color","Cor ao pressionar",0,"Tema","Branco","Azul","Verde","Rosa"),number("opacity","Opacidade do fundo",190,0,255,1),
   number("x","Posicao horizontal (%)",2,0,100,.1),number("y","Posicao vertical (%)",35,0,100,.1));
 }
 public Feature feature(String id){for(Feature f:features)if(f.id.equalsIgnoreCase(id)||f.name.equalsIgnoreCase(id))return f;throw new IllegalArgumentException("Modulo desconhecido: "+id);}
 public int accent(){Feature t=feature("theme");return t.flag("rainbow")?java.awt.Color.HSBtoRGB((System.currentTimeMillis()%8000)/8000f,.6f,1):new int[]{0xFF249BFF,0xFF72A7FF,0xFF58D9E8,0xFF72D9A0,0xFFFF83B7,0xFFFFB266}[t.setting("accent").integer()];}
 @SubscribeEvent public void key(InputEvent.KeyInputEvent event){
  if(!Keyboard.getEventKeyState()||Keyboard.isRepeatEvent()||mc.currentScreen!=null)return;int key=Keyboard.getEventKey();
  if(key==menuKey.getKeyCode()){mc.displayGuiScreen(new ClickGui(this));return;}if(key==panicKey.getKeyCode()){panic();return;}
  if(key==editorKey.getKeyCode()){mc.displayGuiScreen(new HudEditor(this,null));return;}
  for(Feature f:features)if(f.key>0&&f.key==key)f.toggle();profiles.saveAutomatic();
 }
 @SubscribeEvent public void mouse(InputEvent.MouseInputEvent event){if(mc.thePlayer!=null&&mc.currentScreen==null&&Mouse.getEventButtonState())keystrokes.click(Mouse.getEventButton());}
 public void panic(){for(Feature f:features)f.setEnabled(false);profiles.saveAutomatic();message("Todos os modulos desligados.");}
 public void message(String text){if(mc.thePlayer!=null)mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7d[Zyon] \u00a7f"+text));}
}
