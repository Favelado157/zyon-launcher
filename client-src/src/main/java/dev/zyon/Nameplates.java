package dev.zyon;

import java.io.InputStream;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import dev.zyon.platform.event.RenderPlayerEvent;
import dev.zyon.platform.event.RenderGameOverlayEvent;
import dev.zyon.platform.event.SubscribeEvent;
import dev.zyon.platform.ReflectionHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Local identification only. Never changes player names, teams or server packets. */
public final class Nameplates implements IResourceManagerReloadListener {
 private static final ResourceLocation BADGE=new ResourceLocation("zyon","textures/brand/badge.png");
 private final Minecraft mc=Minecraft.getMinecraft();private final Zyon client;private boolean artworkAvailable;private net.minecraft.client.gui.GuiIngame installedGui;
 public Nameplates(Zyon client){
  this.client=client;
  if(mc.getResourceManager() instanceof IReloadableResourceManager)((IReloadableResourceManager)mc.getResourceManager()).registerReloadListener(this);
  else onResourceManagerReload(mc.getResourceManager());
  installTab();
 }
 @Override public void onResourceManagerReload(IResourceManager resources){
  try(InputStream in=resources.getResource(BADGE).getInputStream()){artworkAvailable=in!=null;}
  catch(Exception unavailable){artworkAvailable=false;}
 }
 private boolean badge(){return client.feature("badge").enabled()&&artworkAvailable;}
 private boolean own(NetworkPlayerInfo info){return mc.thePlayer!=null&&mc.thePlayer.getUniqueID().equals(info.getGameProfile().getId());}
 @SubscribeEvent public void overlay(RenderGameOverlayEvent.Pre event){if(mc.ingameGUI!=null&&installedGui!=mc.ingameGUI)installTab();}
 private void installTab(){
  if(mc.ingameGUI==null)return;
  installedGui=mc.ingameGUI;
  try{Field field=ReflectionHelper.findField(net.minecraft.client.gui.GuiIngame.class,"overlayPlayerList","field_175196_v");
   GuiPlayerTabOverlay old=(GuiPlayerTabOverlay)field.get(mc.ingameGUI);
   // Respect an overlay another mod has already installed.
   if(old!=null&&old.getClass()!=GuiPlayerTabOverlay.class)return;
   Tab next=new Tab();
   if(old!=null){for(String[] names:new String[][]{{"header","field_175256_i"},{"footer","field_175255_h"}}){
    Field part=ReflectionHelper.findField(GuiPlayerTabOverlay.class,names);part.set(next,part.get(old));
   }}
   field.set(mc.ingameGUI,next);
  }catch(Exception error){org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Insignia TAB indisponivel; a lista original foi mantida.",error);}
 }
 private void icon(float x,float y,int size){
  mc.getTextureManager().bindTexture(BADGE);GlStateManager.color(1,1,1,1);
  Gui.drawModalRectWithCustomSizedTexture((int)x,(int)y,0,0,size,size,size,size);
 }
 private final class Tab extends GuiPlayerTabOverlay {
  Tab(){super(Nameplates.this.mc,Nameplates.this.mc.ingameGUI);}
  @Override public String getPlayerName(NetworkPlayerInfo info){String original=super.getPlayerName(info);return own(info)&&badge()?"   "+original:original;}
  @Override protected void drawPing(int width,int x,int y,NetworkPlayerInfo info){
   super.drawPing(width,x,y,info);
   if(!own(info)||!badge()||mc.gameSettings.hideGUI)return;
   boolean head=mc.isIntegratedServerRunning()||mc.getNetHandler().getNetworkManager().getIsencrypted();
   int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
   boolean blended=GL11.glIsEnabled(GL11.GL_BLEND);
   int sr=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dr=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),sa=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),da=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
   try{GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);icon(x+(head?9:0),y-1,10);}
   finally{GlStateManager.bindTexture(previous);GlStateManager.tryBlendFuncSeparate(sr,dr,sa,da);if(!blended)GlStateManager.disableBlend();GlStateManager.color(1,1,1,1);}
  }
 }
 @SubscribeEvent public void player(RenderPlayerEvent.Post event){
  if(mc.thePlayer==null||mc.getRenderViewEntity()!=mc.thePlayer)return;
  Feature tags=client.feature("nametag");if(tags.enabled()&&(tags.flag("hide")||!tags.flag("own")))return;
  if(!NametagPolicy.selfVisible(client.feature("selfname").enabled(),mc.gameSettings.thirdPersonView,
      event.entityPlayer==mc.thePlayer,mc.gameSettings.hideGUI,event.entityPlayer.isInvisible(),event.entityPlayer.isSneaking(),
      event.entityPlayer.isSpectator(),event.entityPlayer.isPlayerSleeping()))return;
  String name=event.entityPlayer.getDisplayName().getFormattedText();boolean emblem=badge();
  int textWidth=mc.fontRendererObj.getStringWidth(name),left=NametagPolicy.labelLeft(textWidth,emblem);
  boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),light=GL11.glIsEnabled(GL11.GL_LIGHTING),blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
          texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D),depthWrite=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
  int bound=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),sr=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dr=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
      sa=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),da=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
  RenderManager manager=mc.getRenderManager();float scale=(float)(.02666667*client.feature("selfname").number("scale"));
  GlStateManager.pushMatrix();
  try{
   GlStateManager.translate(event.x,event.y+event.entityPlayer.height+.5,event.z);
   GlStateManager.rotate(-manager.playerViewY,0,1,0);GlStateManager.rotate(manager.playerViewX*(mc.gameSettings.thirdPersonView==2?-1:1),1,0,0);
   GlStateManager.scale(-scale,-scale,scale);GlStateManager.disableLighting();GlStateManager.disableCull();GlStateManager.enableDepth();GlStateManager.depthMask(false);
   GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);
   Gui.drawRect(left-2,-2,left+textWidth+(emblem?12:0)+2,10,0x7005101d);
   GlStateManager.enableBlend();
   if(emblem)icon(left,-1,10);
   mc.fontRendererObj.drawString(name,left+(emblem?12:0),0,0xFFFFFFFF);
  }finally{
   GlStateManager.color(1,1,1,1);GlStateManager.depthMask(depthWrite);GlStateManager.tryBlendFuncSeparate(sr,dr,sa,da);
   if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();
   if(light)GlStateManager.enableLighting();else GlStateManager.disableLighting();
   if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();
   if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();
   if(texture)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
   GlStateManager.bindTexture(bound);GlStateManager.popMatrix();
  }
 }
}
