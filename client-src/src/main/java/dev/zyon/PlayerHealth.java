package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Health from ordinary tracked entities, never discovered through walls or via extra packets. */
public final class PlayerHealth {
 private final Zyon client;private final Minecraft mc=Minecraft.getMinecraft();
 public PlayerHealth(Zyon client){this.client=client;}
 private boolean teamAllows(EntityPlayer p){Team team=p.getTeam();if(team==null)return true;Team own=mc.thePlayer.getTeam();switch(team.getNameTagVisibility()){case NEVER:return false;case HIDE_FOR_OTHER_TEAMS:return own==null||team.isSameTeam(own);case HIDE_FOR_OWN_TEAM:return own==null||!team.isSameTeam(own);default:return true;}}
 @SubscribeEvent public void player(RenderPlayerEvent.Post event){
  if(mc.thePlayer==null||mc.getRenderViewEntity()!=mc.thePlayer||mc.currentScreen!=null&&!(mc.currentScreen instanceof GuiChat))return;EntityPlayer p=event.entityPlayer;Feature f=client.feature("health"),tags=client.feature("nametag");boolean self=p==mc.thePlayer,allowed=teamAllows(p)&&(!tags.enabled()||!tags.flag("hide")&&(!self||tags.flag("own")))&&(self?client.feature("selfname").enabled():mc.thePlayer.canEntityBeSeen(p));
  if(!HealthPolicy.visible(f.enabled(),self,mc.gameSettings.thirdPersonView,mc.gameSettings.hideGUI,p.isInvisible(),p.isSneaking(),p.isPlayerSleeping(),p.isSpectator(),mc.thePlayer.getDistanceSqToEntity(p),allowed))return;
  boolean icon=f.setting("icon").integer()!=2;String value=HealthPolicy.value(p.getHealth(),p.getMaxHealth(),p.getAbsorptionAmount(),f.setting("mode").integer());int w=mc.fontRendererObj.getStringWidth(value)+(icon?12:0),left=-w/2;
  boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),light=GL11.glIsEnabled(GL11.GL_LIGHTING),blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);int bound=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),sr=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dr=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),sa=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),da=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
  RenderManager manager=mc.getRenderManager();float scale=(float)(.02666667*f.number("scale"));double extra=!self&&mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(2)!=null?.3:0;
  GlStateManager.pushMatrix();try{GlStateManager.translate(event.x,event.y+p.height+HealthPolicy.height(f.number("scale"),extra>0),event.z);GlStateManager.rotate(-manager.playerViewY,0,1,0);GlStateManager.rotate(manager.playerViewX*(mc.gameSettings.thirdPersonView==2?-1:1),1,0,0);GlStateManager.scale(-scale,-scale,scale);GlStateManager.disableCull();GlStateManager.disableLighting();GlStateManager.enableDepth();GlStateManager.depthMask(false);GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);Gui.drawRect(left-3,-2,left+w+3,11,0x9005101D);GlStateManager.enableBlend();GlStateManager.enableTexture2D();
   if(icon){boolean hardcore=f.setting("hardcore").integer()==1||f.setting("hardcore").integer()==0&&mc.theWorld.getWorldInfo().isHardcoreModeEnabled();mc.getTextureManager().bindTexture(Gui.icons);GlStateManager.color(1,1,1,1);Gui.drawModalRectWithCustomSizedTexture(left,0,52,hardcore?45:0,9,9,256,256);}mc.fontRendererObj.drawString(value,left+(icon?12:0),0,f.setting("text").rgb());
  }finally{GlStateManager.depthMask(mask);GlStateManager.tryBlendFuncSeparate(sr,dr,sa,da);if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();if(light)GlStateManager.enableLighting();else GlStateManager.disableLighting();if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();if(texture)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();GlStateManager.bindTexture(bound);GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();}
 }
}
