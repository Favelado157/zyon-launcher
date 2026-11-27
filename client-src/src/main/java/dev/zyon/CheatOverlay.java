// Java / Overlay.java / Java 8 / Minecraft 1.8.9
package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.entity.*;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import java.util.*;
public final class CheatOverlay {
 private final Minecraft mc=Minecraft.getMinecraft();private final Zyon client;private final Engine engine;
 public CheatOverlay(Zyon client,Engine engine){this.client=client;this.engine=engine;}private Feature f(String id){return client.feature(id);}
 private void text(String value,int x,int y,int color){mc.fontRendererObj.drawStringWithShadow(value,x,y,color);}
 public void world(float partial){
  if(mc.thePlayer==null||mc.theWorld==null||mc.gameSettings.hideGUI||(!f("esp").enabled()&&!f("tracers").enabled()))return;
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
