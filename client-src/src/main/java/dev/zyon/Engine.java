// Java / Engine.java / Java 8 / Forge 1.8.9
package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;
import java.lang.reflect.Field;
public final class Engine {
 private final Minecraft mc=Minecraft.getMinecraft();private final Zyon client;private final Overlay overlay;
 public EntityLivingBase target;private EntityPlayerSP previous;private Float gamma,step;private long nextAttack;
 private int toolSlot=-1,foodSlot=-1;private boolean ownsSneak,ownsUse,placeAttempted;private Field placeDelay;
 public Engine(Zyon client){this.client=client;overlay=new Overlay(client,this);}private Feature f(String id){return client.feature(id);}
 public void changed(Feature feature,boolean enabled){
  String id=feature.id;
  if(id.equals("fullbright")){if(enabled&&gamma==null)gamma=mc.gameSettings.gammaSetting;if(!enabled&&gamma!=null){mc.gameSettings.gammaSetting=gamma;gamma=null;}}
  if(enabled)return;
  if(id.equals("step"))restoreStep();if(id.equals("safewalk"))releaseSneak();if(id.equals("autoeat"))stopEating();if(id.equals("autotool"))restoreTool();
  if(id.equals("autosprint")&&mc.thePlayer!=null)mc.thePlayer.setSprinting(false);
  if(id.equals("killaura")||id.equals("triggerbot"))target=null;
 }
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END)return;
  if(client.requestMenu&&mc.currentScreen==null){client.requestMenu=false;mc.displayGuiScreen(new ClickGui(client));}
  if(f("fullbright").enabled())mc.gameSettings.gammaSetting=(float)f("fullbright").number("gamma");
  if(mc.thePlayer!=previous){restoreStep();stopEating();restoreTool();releaseSneak();previous=mc.thePlayer;target=null;nextAttack=0;}
  if(mc.thePlayer==null||mc.theWorld==null)return;
  if(mc.currentScreen!=null||!mc.inGameHasFocus||!Display.isActive()||mc.thePlayer.isDead){target=null;stopEating();restoreTool();releaseSneak();return;}
  movement();utilities();if(foodSlot>=0||mc.thePlayer.isUsingItem()){target=null;return;}combat();
 }
 private void movement(){
  EntityPlayerSP p=mc.thePlayer;if(p.isRiding()||p.isSpectator()){releaseSneak();return;}
  boolean moving=p.movementInput.moveForward!=0||p.movementInput.moveStrafe!=0;
  if(f("autosprint").enabled()&&moving&&!p.isSneaking()&&!p.isCollidedHorizontally&&p.getFoodStats().getFoodLevel()>6&&(f("autosprint").flag("omni")||p.movementInput.moveForward>0))p.setSprinting(true);
  if(f("fly").enabled()){horizontal(f("fly").number("speed"));p.motionY=(mc.gameSettings.keyBindJump.isKeyDown()?f("fly").number("vertical"):0)-(physical(mc.gameSettings.keyBindSneak)?f("fly").number("vertical"):0);}
  else if(f("speed").enabled()&&!p.isInWater()&&!p.isInLava()&&!p.isOnLadder()&&!p.isSneaking()){horizontal(f("speed").number("speed"));if(moving&&p.onGround&&f("speed").flag("bhop"))p.jump();}
  if(!f("fly").enabled()&&f("autojump").enabled()&&moving&&p.onGround&&!p.isInWater()&&!p.isInLava()&&!p.isOnLadder()&&!p.isSneaking())p.jump();
  if(f("step").enabled()){if(step==null)step=p.stepHeight;p.stepHeight=(float)f("step").number("height");}
  if(f("safewalk").enabled()&&p.onGround&&moving&&!f("fly").enabled()){
   double a=p.movementInput.moveForward,b=p.movementInput.moveStrafe,len=Math.sqrt(a*a+b*b),yaw=Math.toRadians(p.rotationYaw),ahead=f("safewalk").number("lookahead");
   double dx=(b*Math.cos(yaw)-a*Math.sin(yaw))/len*ahead,dz=(a*Math.cos(yaw)+b*Math.sin(yaw))/len*ahead;
   if(mc.theWorld.getCollidingBoundingBoxes(p,p.getEntityBoundingBox().offset(dx,-.6,dz).contract(.05,0,.05)).isEmpty()){KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(),true);ownsSneak=true;}else releaseSneak();
  }else releaseSneak();
 }
 private void horizontal(double speed){EntityPlayerSP p=mc.thePlayer;double a=p.movementInput.moveForward,b=p.movementInput.moveStrafe,len=Math.sqrt(a*a+b*b),yaw=Math.toRadians(p.rotationYaw);if(len<.001){p.motionX=p.motionZ=0;return;}p.motionX=(b*Math.cos(yaw)-a*Math.sin(yaw))/len*speed;p.motionZ=(a*Math.cos(yaw)+b*Math.sin(yaw))/len*speed;}
 private void combat(){
  target=null;
  if(f("killaura").enabled()&&weapon(f("killaura"))){target=findTarget(f("killaura").number("range"),f("killaura").number("fov"));if(target!=null){if(f("killaura").flag("rotate"))aim(target,180);attack(target,f("killaura").number("cps"));return;}}
  if(f("aimassist").enabled()&&Mouse.isButtonDown(0)&&weapon(f("aimassist"))){EntityLivingBase t=findTarget(f("aimassist").number("range"),f("aimassist").number("fov"));if(t!=null)aim(t,(float)f("aimassist").number("turn"));}
  EntityLivingBase pointed=mc.objectMouseOver!=null&&mc.objectMouseOver.entityHit instanceof EntityLivingBase?(EntityLivingBase)mc.objectMouseOver.entityHit:null;
  if(f("triggerbot").enabled()&&weapon(f("triggerbot"))&&pointed!=null&&valid(pointed,f("triggerbot").number("range"),360)){target=pointed;attack(target,f("triggerbot").number("cps"));return;}
  if(f("autoclicker").enabled()&&Mouse.isButtonDown(0)&&weapon(f("autoclicker"))&&(mc.objectMouseOver==null||mc.objectMouseOver.typeOfHit!=MovingObjectPosition.MovingObjectType.BLOCK)){
   if(pointed!=null){if(valid(pointed,mc.playerController.getBlockReachDistance(),360))attack(pointed,f("autoclicker").number("cps"));}else if(ready(f("autoclicker").number("cps")))mc.thePlayer.swingItem();
  }
 }
 private boolean weapon(Feature feature){if(!feature.flag("weapon"))return true;ItemStack item=mc.thePlayer.getHeldItem();return item!=null&&(item.getItem() instanceof ItemSword||item.getItem() instanceof ItemAxe);}
 public boolean selectedType(EntityLivingBase entity,Feature feature){
  if(entity==mc.thePlayer||entity.isDead||entity.getHealth()<=0||entity instanceof EntityArmorStand)return false;
  if(entity instanceof EntityPlayer)return feature.flag("players")&&!((EntityPlayer)entity).isSpectator();
  if(entity instanceof IMob)return feature.flag("mobs");return entity instanceof EntityAnimal&&feature.flag("animals");
 }
 private boolean valid(EntityLivingBase e,double range,double fov){
  Feature a=f("killaura");if(!selectedType(e,a)||(!a.flag("invisible")&&e.isInvisible())||mc.thePlayer.getDistanceToEntity(e)>range||(!a.flag("walls")&&!mc.thePlayer.canEntityBeSeen(e)))return false;
  if(fov>=360)return true;float yaw=(float)Math.toDegrees(Math.atan2(e.posZ-mc.thePlayer.posZ,e.posX-mc.thePlayer.posX))-90;return Math.abs(MathHelper.wrapAngleTo180_float(yaw-mc.thePlayer.rotationYaw))<=fov/2;
 }
 private EntityLivingBase findTarget(double range,double fov){EntityLivingBase best=null;double score=Double.MAX_VALUE;for(Entity e:mc.theWorld.loadedEntityList)if(e instanceof EntityLivingBase){EntityLivingBase living=(EntityLivingBase)e;if(!valid(living,range,fov))continue;double n=f("killaura").setting("sort").integer()==1?living.getHealth():mc.thePlayer.getDistanceSqToEntity(living);if(n<score){best=living;score=n;}}return best;}
 private void aim(EntityLivingBase e,float maximum){
  double dx=e.posX-mc.thePlayer.posX,dz=e.posZ-mc.thePlayer.posZ,dy=e.posY+e.getEyeHeight()*.85-mc.thePlayer.posY-mc.thePlayer.getEyeHeight();
  float yaw=(float)Math.toDegrees(Math.atan2(dz,dx))-90,pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
  mc.thePlayer.rotationYaw+=MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(yaw-mc.thePlayer.rotationYaw),-maximum,maximum);
  mc.thePlayer.rotationPitch=MathHelper.clamp_float(mc.thePlayer.rotationPitch+MathHelper.clamp_float(pitch-mc.thePlayer.rotationPitch,-maximum,maximum),-90,90);
 }
 private boolean ready(double cps){long now=System.nanoTime();if(now<nextAttack)return false;nextAttack=now+(long)(1e9/cps);return true;}
 private void attack(EntityLivingBase e,double cps){if(ready(cps)){mc.thePlayer.swingItem();mc.playerController.attackEntity(mc.thePlayer,e);}}
 private void utilities(){
  if(f("autoeat").enabled())eat();else stopEating();if(foodSlot>=0)return;
  if(f("autotool").enabled()&&Mouse.isButtonDown(0)&&mc.objectMouseOver!=null&&mc.objectMouseOver.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK){
   net.minecraft.block.Block block=mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();ItemStack held=mc.thePlayer.getHeldItem();float bestSpeed=held==null?1:held.getStrVsBlock(block);int best=mc.thePlayer.inventory.currentItem;
   for(int i=0;i<9;i++){ItemStack item=mc.thePlayer.inventory.getStackInSlot(i);if(item!=null&&item.getStrVsBlock(block)>bestSpeed){bestSpeed=item.getStrVsBlock(block);best=i;}}
   if(best!=mc.thePlayer.inventory.currentItem){if(toolSlot<0)toolSlot=mc.thePlayer.inventory.currentItem;mc.thePlayer.inventory.currentItem=best;mc.playerController.updateController();}
  }else restoreTool();
  if(f("fastplace").enabled()&&mc.thePlayer.getHeldItem()!=null&&mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)try{
   if(!placeAttempted){placeAttempted=true;placeDelay=ReflectionHelper.findField(Minecraft.class,"rightClickDelayTimer","field_71467_ac");placeDelay.setAccessible(true);}if(placeDelay==null)throw new IllegalStateException("Campo nao encontrado.");int delay=f("fastplace").setting("delay").integer();if(placeDelay.getInt(mc)>delay)placeDelay.setInt(mc,delay);
  }catch(Exception e){f("fastplace").setEnabled(false);org.apache.logging.log4j.LogManager.getLogger("Zyon").error("FastPlace indisponivel",e);client.message("FastPlace desligado por incompatibilidade; veja latest.log.");}
 }
 private void eat(){
  if(mc.thePlayer.capabilities.isCreativeMode||mc.thePlayer.getFoodStats().getFoodLevel()>f("autoeat").number("hunger")){stopEating();return;}
  if(foodSlot>=0&&mc.thePlayer.getHeldItem()!=null&&mc.thePlayer.getHeldItem().getItem() instanceof ItemFood){KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(),true);return;}
  stopEating();if(mc.thePlayer.isUsingItem()||physical(mc.gameSettings.keyBindUseItem))return;int best=-1,amount=-1;
  for(int i=0;i<9;i++){ItemStack item=mc.thePlayer.inventory.getStackInSlot(i);if(item==null||!(item.getItem() instanceof ItemFood)||item.getItem() instanceof ItemAppleGold)continue;int a=((ItemFood)item.getItem()).getHealAmount(item);if(a>amount){best=i;amount=a;}}
  if(best>=0){restoreTool();foodSlot=mc.thePlayer.inventory.currentItem;mc.thePlayer.inventory.currentItem=best;mc.playerController.updateController();KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(),true);ownsUse=true;}
 }
 private void restoreStep(){if(step!=null&&previous!=null)previous.stepHeight=step;step=null;}
 private void restoreTool(){if(toolSlot>=0&&previous!=null&&f("autotool").flag("restore"))previous.inventory.currentItem=toolSlot;toolSlot=-1;}
 private void stopEating(){if(ownsUse)KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(),physical(mc.gameSettings.keyBindUseItem));if(foodSlot>=0&&previous!=null)previous.inventory.currentItem=foodSlot;ownsUse=false;foodSlot=-1;}
 private void releaseSneak(){if(ownsSneak)KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(),physical(mc.gameSettings.keyBindSneak));ownsSneak=false;}
 private boolean physical(KeyBinding binding){int key=binding.getKeyCode();return key<0?key+100>=0&&key+100<Mouse.getButtonCount()&&Mouse.isButtonDown(key+100):key>0&&key<Keyboard.KEYBOARD_SIZE&&Keyboard.isKeyDown(key);}
 @SubscribeEvent public void hud(RenderGameOverlayEvent.Post event){if(event.type==RenderGameOverlayEvent.ElementType.ALL)overlay.hud();}
 @SubscribeEvent public void world(RenderWorldLastEvent event){overlay.world(event.partialTicks);}
}
