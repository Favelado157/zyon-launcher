package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import java.util.Locale;

public final class Overlay {
 private final Minecraft mc=Minecraft.getMinecraft();private final Zyon client;
 public Overlay(Zyon client){this.client=client;}private Feature f(String id){return client.feature(id);}
 private void text(String value,int x,int y,int color){mc.fontRendererObj.drawStringWithShadow(value,x,y,color);}
 public void hud(){
  if(mc.thePlayer==null||mc.gameSettings.showDebugInfo||mc.gameSettings.hideGUI||mc.currentScreen instanceof ClickGui||mc.currentScreen instanceof HudEditor||mc.currentScreen instanceof KeyLayoutEditor)return;
  ScaledResolution res=new ScaledResolution(mc);int width=res.getScaledWidth(),height=res.getScaledHeight(),accent=client.accent();
  client.keystrokes.draw(width,height,false);Widgets widgets=new Widgets(client);for(Feature feature:client.features)if(Catalog.widget(feature)&&!feature.id.equals("keystrokes")&&feature.enabled())widgets.draw(feature,width,height,false);
  if(f("crosshair").enabled()&&mc.gameSettings.thirdPersonView==0){int cx=width/2,cy=height/2,s=f("crosshair").setting("size").integer(),g=f("crosshair").setting("gap").integer();Gui.drawRect(cx-g-s,cy,cx-g,cy+1,accent);Gui.drawRect(cx+g+1,cy,cx+g+s+1,cy+1,accent);Gui.drawRect(cx,cy-g-s,cx+1,cy-g,accent);Gui.drawRect(cx,cy+g+1,cx+1,cy+g+s+1,accent);if(f("crosshair").flag("dot"))Gui.drawRect(cx,cy,cx+1,cy+1,accent);}
  Feature hud=f("hud");if(!hud.enabled())return;int x=Math.min(hud.setting("x").integer(),Math.max(0,width-150)),y=Math.min(hud.setting("y").integer(),Math.max(0,height-80));
  Gui.drawRect(x,y,x+144,y+34,0xD0091C2F);Gui.drawRect(x,y,x+2,y+34,accent);if(hud.flag("logo"))Branding.image(Branding.LOGO,x+6,y+4,26,26);text("ZYON VISUAL",x+(hud.flag("logo")?39:10),y+7,accent);text("1.8.9 / 1.5",x+(hud.flag("logo")?39:10),y+20,0xFF9DB8D5);int row=y+42;
  if(hud.flag("fps")){text(Minecraft.getDebugFPS()+" FPS",x,row,0xFFE8F4FF);row+=12;}
  if(hud.flag("coords")){text(String.format(Locale.ROOT,"XYZ  %.1f  %.1f  %.1f",mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ),x,row,0xFFE8F4FF);row+=12;}
  if(hud.flag("modules")){int top=12;for(Feature feature:client.features)if(feature.enabled()&&feature.category==Feature.Category.RENDER){int tw=mc.fontRendererObj.getStringWidth(feature.name);Gui.drawRect(width-tw-23,top-3,width-10,top+11,0xC0091C2F);Gui.drawRect(width-12,top-3,width-10,top+11,accent);text(feature.name,width-tw-17,top,accent);top+=15;}}
 }
}
