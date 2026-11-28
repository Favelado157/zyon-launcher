package dev.zyon.platform;
import dev.zyon.*;import dev.zyon.platform.event.*;
import net.minecraft.client.*;import net.minecraft.client.gui.*;import net.minecraft.client.resources.*;import net.minecraft.entity.*;import net.minecraft.entity.player.EntityPlayer;import net.minecraft.util.*;import java.io.*;import java.util.*;
import org.lwjgl.input.*;
public final class Hooks {
 private static boolean initialized,added,keyboardPending,mousePending;
 private static Minecraft mc(){return Minecraft.getMinecraft();}
 public static void resources(){if(added)return;try{File file=new File(Hooks.class.getProtectionDomain().getCodeSource().getLocation().toURI());List<IResourcePack> packs=(List<IResourcePack>)ReflectionHelper.findField(Minecraft.class,"defaultResourcePacks").get(mc());packs.add(file.isDirectory()?new FolderResourcePack(file):new FileResourcePack(file));added=true;}catch(Exception e){throw new RuntimeException("Zyon resources unavailable",e);}}
 private static void ensure(){if(initialized)return;initialized=true;try{new Zyon().init();System.out.println("ZYON_CLIENT_ENGINE_READY vanilla-1.8.9");}catch(Throwable e){initialized=false;throw new RuntimeException("Zyon initialization failed",e);}}
 public static GuiScreen guiOpen(GuiScreen screen){if(screen==null&&mc().theWorld==null)screen=new GuiMainMenu();if(screen instanceof GuiMainMenu)ensure();if(!initialized)return screen;return Platform.EVENTS.post(new GuiOpenEvent(screen)).gui;}
 public static void tickStart(){if(initialized)Platform.EVENTS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));}
 public static void tickEnd(){if(initialized){ClientPresence.tick();Platform.EVENTS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));}}
 public static void renderStart(){if(initialized)Platform.EVENTS.post(new TickEvent.RenderTickEvent(TickEvent.Phase.START));}
 public static void worldLast(float partial){if(initialized)Platform.EVENTS.post(new RenderWorldLastEvent(partial));}
 public static net.minecraft.util.ResourceLocation cape(net.minecraft.util.ResourceLocation original,net.minecraft.client.entity.AbstractClientPlayer player){return initialized?CapeSupport.location(original,player):original;}
 public static void citAtlas(net.minecraft.client.renderer.texture.TextureMap atlas,net.minecraft.client.resources.IResourceManager manager){ArmorCit.prepare(atlas,manager);}
 public static net.minecraft.util.ResourceLocation citResource(net.minecraft.util.ResourceLocation vanilla,net.minecraft.util.ResourceLocation sprite,int level){return ArmorCit.atlasResource(vanilla,sprite,level);}
 public static net.minecraft.util.ResourceLocation citArmor(net.minecraft.util.ResourceLocation vanilla,net.minecraft.item.ItemStack item,int slot){return ArmorCit.armorTexture(vanilla,item,slot);}
 public static net.minecraft.client.resources.model.IBakedModel citItem(net.minecraft.client.resources.model.IBakedModel vanilla,net.minecraft.item.ItemStack item){return ArmorCit.itemModel(vanilla,item);}
 public static void connectionFault(final Throwable failure){org.apache.logging.log4j.LogManager.getLogger("Zyon").error("Connection integration failed",failure);mc().addScheduledTask(new Runnable(){public void run(){mc().displayGuiScreen(new GuiDisconnected(new GuiMultiplayer(new GuiMainMenu()),"connect.failed",new ChatComponentText("Falha interna ao iniciar a conexao. Atualize o Zyon e tente novamente. Detalhes em logs/latest.log.")));}});}
 public static void renderEnd(){if(initialized)Platform.EVENTS.post(new TickEvent.RenderTickEvent(TickEvent.Phase.END));}
 public static boolean keyboardNext(){if(keyboardPending&&initialized){keyboardPending=false;Platform.EVENTS.post(new InputEvent.KeyInputEvent());}boolean next=Keyboard.next();keyboardPending=next;return next;}
 public static boolean mouseNext(){if(mousePending&&initialized){mousePending=false;Platform.EVENTS.post(new InputEvent.MouseInputEvent());}boolean next=Mouse.next();mousePending=next;return next;}
 public static void overlayPre(){if(initialized)Platform.EVENTS.post(new RenderGameOverlayEvent.Pre(RenderGameOverlayEvent.ElementType.ALL,0));}
 public static void overlayPost(float partial){if(initialized)Platform.EVENTS.post(new RenderGameOverlayEvent.Post(RenderGameOverlayEvent.ElementType.ALL,partial));}
 public static boolean crosshair(boolean vanilla){return vanilla&&(!initialized||!Zyon.instance.feature("crosshair").enabled());}
 public static void lightmap(int unit,float x,float y){if(unit==net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit){Platform.lastBrightnessX=x;Platform.lastBrightnessY=y;}}
 public static float fov(float f){return initialized?Platform.EVENTS.post(new EntityViewRenderEvent.FOVModifier(f)).getFOV():f;}
 public static void playerPost(EntityPlayer p,double x,double y,double z,float partial){if(initialized)Platform.EVENTS.post(new RenderPlayerEvent.Post(p,x,y,z,partial));}
 public static boolean hideName(EntityLivingBase entity){return initialized&&Platform.EVENTS.post(new RenderLivingEvent.Specials.Pre(entity)).isCanceled();}
 public static boolean highlight(EntityPlayer p,MovingObjectPosition target,int sub,float partial){return initialized&&Platform.EVENTS.post(new DrawBlockHighlightEvent(p,target,sub,partial)).isCanceled();}
 public static void guiInit(GuiScreen screen){if(initialized)try{List<GuiButton> buttons=(List<GuiButton>)ReflectionHelper.findField(GuiScreen.class,"buttonList").get(screen);Platform.EVENTS.post(new GuiScreenEvent.InitGuiEvent.Post(screen,buttons));}catch(Exception e){throw new RuntimeException(e);}}
 public static boolean guiMouse(GuiScreen screen){return initialized&&Platform.EVENTS.post(new GuiScreenEvent.MouseInputEvent.Pre(screen)).isCanceled();}
 public static void drawGui(GuiScreen screen,int x,int y,float partial){if(!initialized){screen.drawScreen(x,y,partial);return;}if(!Platform.EVENTS.post(new GuiScreenEvent.DrawScreenEvent.Pre(screen,x,y,partial)).isCanceled())screen.drawScreen(x,y,partial);Platform.EVENTS.post(new GuiScreenEvent.DrawScreenEvent.Post(screen,x,y,partial));}
 public static void guiAction(GuiScreen screen,GuiButton button){if(initialized&&Platform.EVENTS.post(new GuiScreenEvent.ActionPerformedEvent.Pre(screen,button)).isCanceled())return;try{ReflectionHelper.findMethod(GuiScreen.class,screen,new String[]{"actionPerformed"},GuiButton.class).invoke(screen,button);}catch(Exception e){throw new RuntimeException(e);}}
 public static boolean chat(String text,boolean history){if(!initialized||text==null||!text.matches("(?s)^/zyon(?:\\s.*)?$"))return false;if(history)mc().ingameGUI.getChatGUI().addToSentMessages(text);String rest=text.substring(5).trim();try{Platform.command.processCommand(mc().thePlayer,rest.isEmpty()?new String[0]:rest.split("\\s+"));}catch(Exception e){Zyon.instance.message("Comando invalido. Use /zyon gui.");}return true;}
}
