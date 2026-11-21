package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;

/** These effects only change local rendering. No entity state or network input is modified. */
public final class LocalEffects {
 private final Zyon c;private final Minecraft mc=Minecraft.getMinecraft();
 public LocalEffects(Zyon client){c=client;}
 @SubscribeEvent public void inventory(GuiScreenEvent.DrawScreenEvent.Pre e){if(c.feature("classicpvp").enabled()&&c.feature("classicpvp").flag("inventory")&&e.gui instanceof GuiInventory)try{ReflectionHelper.findField(GuiContainer.class,"guiLeft","field_147003_i").setInt(e.gui,(e.gui.width-176)/2);}catch(Exception ignored){}}
 @SubscribeEvent public void tags(RenderLivingEvent.Specials.Pre e){if(e.entity instanceof EntityPlayer&&c.feature("nametag").enabled()&&c.feature("nametag").flag("hide"))e.setCanceled(true);}
 @SubscribeEvent public void highlight(DrawBlockHighlightEvent e){Feature f=c.feature("blockoverlay");if(!f.enabled()||e.target==null||e.target.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK||e.subID!=0)return;if(!f.flag("customOutline")&&!f.flag("customFill"))return;BlockPos pos=e.target.getBlockPos();Block block=mc.theWorld.getBlockState(pos).getBlock();if(block.getMaterial()==Material.air||!mc.theWorld.getWorldBorder().contains(pos))return;block.setBlockBoundsBasedOnState(mc.theWorld,pos);AxisAlignedBB a=block.getSelectedBoundingBox(mc.theWorld,pos);if(a==null)return;double px=e.player.lastTickPosX+(e.player.posX-e.player.lastTickPosX)*e.partialTicks,py=e.player.lastTickPosY+(e.player.posY-e.player.lastTickPosY)*e.partialTicks,pz=e.player.lastTickPosZ+(e.player.posZ-e.player.lastTickPosZ)*e.partialTicks;a=a.expand(.002,.002,.002).offset(-px,-py,-pz);
  // All state below uses raw GL and is restored, leaving GlStateManager's cache unchanged.
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);try{GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_CULL_FACE);if(f.flag("customFill")){color(f.setting("fillColor").integer(),f.setting("fillAlpha").integer()/255f);fill(a);}if(f.flag("customOutline")){color(f.setting("outlineColor").integer(),1);GL11.glLineWidth((float)f.number("width"));RenderGlobal.drawSelectionBoundingBox(a);}}finally{GL11.glPopAttrib();}if(f.flag("customOutline"))e.setCanceled(true);
 }
 private static void color(int rgb,float alpha){GL11.glColor4f((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f,alpha);}
 private static void fill(AxisAlignedBB a){double x=a.minX,y=a.minY,z=a.minZ,X=a.maxX,Y=a.maxY,Z=a.maxZ;GL11.glBegin(GL11.GL_QUADS);v(x,y,z);v(X,y,z);v(X,Y,z);v(x,Y,z);v(x,y,Z);v(x,Y,Z);v(X,Y,Z);v(X,y,Z);v(x,y,z);v(x,Y,z);v(x,Y,Z);v(x,y,Z);v(X,y,z);v(X,y,Z);v(X,Y,Z);v(X,Y,z);v(x,y,z);v(x,y,Z);v(X,y,Z);v(X,y,z);v(x,Y,z);v(X,Y,z);v(X,Y,Z);v(x,Y,Z);GL11.glEnd();}private static void v(double x,double y,double z){GL11.glVertex3d(x,y,z);}
 public static final class Brightness {
  private final Zyon c;private final Minecraft mc=Minecraft.getMinecraft();private final BrightnessState state=new BrightnessState();private boolean rendererChecked;
  public Brightness(Zyon c){this.c=c;}
  @SubscribeEvent public void render(TickEvent.RenderTickEvent e){mc.gameSettings.gammaSetting=state.apply(mc.gameSettings.gammaSetting,e.phase==TickEvent.Phase.START&&mc.theWorld!=null&&c.feature("fullbright").enabled());}
  @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;mc.gameSettings.gammaSetting=state.apply(mc.gameSettings.gammaSetting,false);if(!rendererChecked&&mc.theWorld!=null){rendererChecked=true;ClassicAnimations.install(c);Render<?> old=mc.getRenderManager().getEntityClassRenderObject(EntityItem.class);if(old!=null&&old.getClass()==RenderEntityItem.class)mc.getRenderManager().entityRenderMap.put(EntityItem.class,new Dropped(c));}}
 }
 private static final class Dropped extends RenderEntityItem {
  private final Zyon c;private final Minecraft mc=Minecraft.getMinecraft();Dropped(Zyon c){super(Minecraft.getMinecraft().getRenderManager(),Minecraft.getMinecraft().getRenderItem());this.c=c;}
  @Override public void doRender(EntityItem item,double x,double y,double z,float yaw,float partial){if(!c.feature("itemphysics").enabled()){super.doRender(item,x,y,z,yaw,partial);return;}ItemStack stack=item.getEntityItem();if(stack==null)return;GlStateManager.pushMatrix();try{bindTexture(TextureMap.locationBlocksTexture);GlStateManager.translate(x,y+.08,z);GlStateManager.rotate(item.onGround?item.getEntityId()*37%360:(item.ticksExisted+partial)*(float)c.feature("itemphysics").number("speed")*2,0,1,0);GlStateManager.rotate(item.onGround?90:45,1,0,0);GlStateManager.scale(.5,.5,.5);GlStateManager.enableRescaleNormal();GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);mc.getRenderItem().renderItem(stack,ItemCameraTransforms.TransformType.GROUND);}finally{GlStateManager.disableRescaleNormal();GlStateManager.disableBlend();GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();}}
 }
}
