package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

/** Local rendering only: no entity targeting, movement or input automation. */
public final class Visuals {
 private final Minecraft mc=Minecraft.getMinecraft(); private final Zyon client;private final Overlay overlay;
 public Visuals(Zyon client){this.client=client;overlay=new Overlay(client);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
  if(event.phase==TickEvent.Phase.END&&client.requestMenu&&mc.currentScreen==null){client.requestMenu=false;mc.displayGuiScreen(new ClickGui(client));}
 }
 public static float zoomFov(float fov,double factor,boolean held){return held?Math.max(5,fov/(float)factor):fov;}
 @SubscribeEvent public void zoom(EntityViewRenderEvent.FOVModifier event){
  if(client.feature("zoom").enabled()&&mc.thePlayer!=null&&mc.currentScreen==null&&mc.inGameHasFocus&&Display.isActive()&&client.zoomKey.isKeyDown())event.setFOV(zoomFov(event.getFOV(),client.feature("zoom").number("factor"),true));
 }
 @SubscribeEvent public void hud(RenderGameOverlayEvent.Post event){if(event.type==RenderGameOverlayEvent.ElementType.ALL)overlay.hud();}
}
