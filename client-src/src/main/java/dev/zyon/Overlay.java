// Java / Overlay.java / Java 8 / Forge 1.8.9
package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.entity.*;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import java.util.*;
public final class Overlay {
 private final Minecraft mc=Minecraft.getMinecraft();private final Zyon client;private final Engine engine;
 public Overlay(Zyon client,Engine engine){this.client=client;this.engine=engine;}private Feature f(String id){return client.feature(id);}
 private void text(String value,int x,int y,int color){mc.fontRendererObj.drawStringWithShadow(value,x,y,color);}
 public void hud(){
  if(mc.thePlayer==null||mc.gameSettings.showDebugInfo||mc.gameSettings.hideGUI||mc.currentScreen instanceof ClickGui||mc.currentScreen instanceof HudEditor)return;
  ScaledResolution res=new ScaledResolution(mc);int width=res.getScaledWidth(),height=res.getScaledHeight(),accent=client.accent();
  client.keystrokes.draw(width,height,false);
  if(f("crosshair").enabled()&&mc.gameSettings.thirdPersonView==0){int cx=width/2,cy=height/2,s=f("crosshair").setting("size").integer(),g=f("crosshair").setting("gap").integer();Gui.drawRect(cx-g-s,cy,cx-g,cy+1,accent);Gui.drawRect(cx+g+1,cy,cx+g+s+1,cy+1,accent);Gui.drawRect(cx,cy-g-s,cx+1,cy-g,accent);Gui.drawRect(cx,cy+g+1,cx+1,cy+g+s+1,accent);if(f("crosshair").flag("dot"))Gui.drawRect(cx,cy,cx+1,cy+1,accent);}
  Feature hud=f("hud");if(!hud.enabled())return;int x=Math.min(hud.setting("x").integer(),Math.max(0,width-150)),y=Math.min(hud.setting("y").integer(),Math.max(0,height-80));
  Gui.drawRect(x,y,x+144,y+34,0xD0161925);Gui.drawRect(x,y,x+2,y+34,accent);if(hud.flag("logo"))Branding.image(Branding.LOGO,x+6,y+4,26,26);text("ZYON",x+(hud.flag("logo")?39:10),y+7,accent);text("1.8.9 / 1.2",x+(hud.flag("logo")?39:10),y+20,0xFFABB2C8);int row=y+42;
  if(hud.flag("fps")){text(Minecraft.getDebugFPS()+" FPS",x,row,0xFFE1E4EE);row+=12;}
  if(hud.flag("coords")){text(String.format(Locale.ROOT,"XYZ  %.1f  %.1f  %.1f",mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ),x,row,0xFFABB2C8);row+=12;}
  if(hud.flag("target")&&engine.target!=null&&!engine.target.isDead){EntityLivingBase t=engine.target;int top=Math.min(row+10,height-39);Gui.drawRect(x,top,x+150,top+38,0xD0161925);text(mc.fontRendererObj.trimStringToWidth(t.getName(),132),x+8,top+6,0xFFF1F2F8);text(String.format(Locale.ROOT,"%.1f HP  |  %.1f m",t.getHealth(),mc.thePlayer.getDistanceToEntity(t)),x+8,top+18,0xFFABB2C8);Gui.drawRect(x+8,top+31,x+142,top+33,0xFF34394D);float ratio=Math.max(0,Math.min(1,t.getHealth()/Math.max(1,t.getMaxHealth())));Gui.drawRect(x+8,top+31,x+8+(int)(134*ratio),top+33,accent);}
  if(hud.flag("modules")){
   List<Feature> active=new ArrayList<Feature>();for(Feature a:client.features)if(a.enabled()&&a.category!=Feature.Category.CLIENT)active.add(a);
   Collections.sort(active,new Comparator<Feature>(){public int compare(Feature a,Feature b){return Integer.compare(mc.fontRendererObj.getStringWidth(b.name),mc.fontRendererObj.getStringWidth(a.name));}});
   int top=12;for(Feature a:active){int tw=mc.fontRendererObj.getStringWidth(a.name);Gui.drawRect(width-tw-23,top-3,width-10,top+11,0xC0161925);Gui.drawRect(width-12,top-3,width-10,top+11,accent);text(a.name,width-tw-17,top,accent);top+=15;if(top>height-20)break;}
  }
 }
 public void world(float partial){
  if(mc.thePlayer==null||mc.theWorld==null||(!f("esp").enabled()&&!f("tracers").enabled()))return;
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();try{
   GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthMask(false);GL11.glEnable(GL11.GL_LINE_SMOOTH);
   if(f("esp").enabled())entities(f("esp"),partial,true);if(f("tracers").enabled())entities(f("tracers"),partial,false);
  }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
 }
 private double lerp(double a,double b,float p){return a+(b-a)*p;}
 private void entities(Feature feature,float partial,boolean boxes){
  if(feature.flag("walls"))GL11.glDisable(GL11.GL_DEPTH_TEST);else GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glLineWidth(boxes?(float)feature.number("width"):1);
  double ox=mc.getRenderManager().viewerPosX,oy=mc.getRenderManager().viewerPosY,oz=mc.getRenderManager().viewerPosZ;Entity view=mc.getRenderViewEntity();if(view==null)return;Vec3 look=view.getLook(partial);
  double sx=lerp(view.lastTickPosX,view.posX,partial)-ox+look.xCoord*.1,sy=lerp(view.lastTickPosY,view.posY,partial)-oy+view.getEyeHeight()+look.yCoord*.1,sz=lerp(view.lastTickPosZ,view.posZ,partial)-oz+look.zCoord*.1;
  int color=client.accent();GL11.glColor4f(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,.9f);GL11.glBegin(GL11.GL_LINES);
  try{for(Entity e:mc.theWorld.loadedEntityList){if(!(e instanceof EntityLivingBase)||!engine.selectedType((EntityLivingBase)e,feature)||mc.thePlayer.getDistanceToEntity(e)>feature.number("distance"))continue;
   double x=lerp(e.lastTickPosX,e.posX,partial)-ox,y=lerp(e.lastTickPosY,e.posY,partial)-oy,z=lerp(e.lastTickPosZ,e.posZ,partial)-oz;
   if(boxes)box(e.getEntityBoundingBox().offset(x-e.posX,y-e.posY,z-e.posZ).expand(.04,.04,.04));else line(sx,sy,sz,x,y+e.height*.5,z);
  }}finally{GL11.glEnd();}
 }
 private void line(double x,double y,double z,double xx,double yy,double zz){GL11.glVertex3d(x,y,z);GL11.glVertex3d(xx,yy,zz);}
 private void box(AxisAlignedBB b){
  line(b.minX,b.minY,b.minZ,b.maxX,b.minY,b.minZ);line(b.maxX,b.minY,b.minZ,b.maxX,b.minY,b.maxZ);line(b.maxX,b.minY,b.maxZ,b.minX,b.minY,b.maxZ);line(b.minX,b.minY,b.maxZ,b.minX,b.minY,b.minZ);
  line(b.minX,b.maxY,b.minZ,b.maxX,b.maxY,b.minZ);line(b.maxX,b.maxY,b.minZ,b.maxX,b.maxY,b.maxZ);line(b.maxX,b.maxY,b.maxZ,b.minX,b.maxY,b.maxZ);line(b.minX,b.maxY,b.maxZ,b.minX,b.maxY,b.minZ);
  line(b.minX,b.minY,b.minZ,b.minX,b.maxY,b.minZ);line(b.maxX,b.minY,b.minZ,b.maxX,b.maxY,b.minZ);line(b.maxX,b.minY,b.maxZ,b.maxX,b.maxY,b.maxZ);line(b.minX,b.minY,b.maxZ,b.minX,b.maxY,b.maxZ);
 }
}
