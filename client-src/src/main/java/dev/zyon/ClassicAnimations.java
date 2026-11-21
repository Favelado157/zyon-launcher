package dev.zyon;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.*;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
/** Uses vanilla item state and transforms. It never changes player input, swing timing or packets. */
public final class ClassicAnimations extends ItemRenderer {
 private final Zyon c;private final Minecraft mc;private final Map<String,Method> methods=new HashMap<String,Method>();private Field stack,progress,previous;
 private ClassicAnimations(Zyon c)throws Exception{super(Minecraft.getMinecraft());this.c=c;mc=Minecraft.getMinecraft();stack=field("itemToRender","field_78453_b");progress=field("equippedProgress","field_78454_c");previous=field("prevEquippedProgress","field_78451_d");
  method("rotateArroundXAndY","func_178101_a",float.class,float.class);method("setLightMapFromPlayer","func_178109_a",AbstractClientPlayer.class);method("rotateWithPlayerRotations","func_178110_a",EntityPlayerSP.class,float.class);method("transformFirstPersonItem","func_178096_b",float.class,float.class);method("performDrinking","func_178104_a",AbstractClientPlayer.class,float.class);method("doBlockTransformations","func_178103_d");method("doBowTransformations","func_178098_a",float.class,AbstractClientPlayer.class);method("doItemUsedTransformations","func_178105_d",float.class);
 }
 private Field field(String name,String srg){return ReflectionHelper.findField(ItemRenderer.class,name,srg);}
 private void method(String name,String srg,Class<?>... types){methods.put(name,ReflectionHelper.findMethod(ItemRenderer.class,this,new String[]{name,srg},types));}
 private void call(String name,Object... args)throws Exception{methods.get(name).invoke(this,args);}
 public static void install(Zyon c){Minecraft mc=Minecraft.getMinecraft();ItemRenderer old=mc.getItemRenderer();if(old==null||old.getClass()!=ItemRenderer.class)return;try{ClassicAnimations next=new ClassicAnimations(c);for(String[] names:new String[][]{{"itemToRender","field_78453_b"},{"equippedProgress","field_78454_c"},{"prevEquippedProgress","field_78451_d"},{"equippedItemSlot","field_78450_g"}}){Field f=ReflectionHelper.findField(ItemRenderer.class,names);f.set(next,f.get(old));}Field game=ReflectionHelper.findField(Minecraft.class,"itemRenderer","field_175620_Y"),renderer=ReflectionHelper.findField(EntityRenderer.class,"itemRenderer","field_78516_c");try{renderer.set(mc.entityRenderer,next);game.set(mc,next);}catch(Exception e){renderer.set(mc.entityRenderer,old);game.set(mc,old);throw e;}}catch(Exception e){org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Animacoes classicas indisponiveis; mantido renderizador original.",e);}}
 @Override public void renderItemInFirstPerson(float partial){Feature f=c.feature("classicpvp");if(!f.enabled()){super.renderItemInFirstPerson(partial);return;}ItemStack item;try{item=(ItemStack)stack.get(this);}catch(Exception e){super.renderItemInFirstPerson(partial);return;}if(item==null||item.getItem() instanceof ItemMap){super.renderItemInFirstPerson(partial);return;}
  EntityPlayerSP p=mc.thePlayer;boolean pushed=false;try{float equipped=f.flag("equip")?0:1-(previous.getFloat(this)+(progress.getFloat(this)-previous.getFloat(this))*partial),swing=p.getSwingProgress(partial);float pitch=p.prevRotationPitch+(p.rotationPitch-p.prevRotationPitch)*partial,yaw=p.prevRotationYaw+(p.rotationYaw-p.prevRotationYaw)*partial;call("rotateArroundXAndY",pitch,yaw);call("setLightMapFromPlayer",p);call("rotateWithPlayerRotations",p,partial);GlStateManager.enableRescaleNormal();GlStateManager.pushMatrix();pushed=true;
   if(f.flag("position")){GlStateManager.translate(-.05f,.02f,-.03f);GlStateManager.scale(.98f,.98f,.98f);}if(f.flag("rod")&&item.getItem() instanceof ItemFishingRod){GlStateManager.translate(.05f,-.03f,0);GlStateManager.rotate(-8,0,1,0);}
   if(p.getItemInUseCount()>0){EnumAction action=item.getItemUseAction();switch(action){case EAT:case DRINK:call("performDrinking",p,partial);call("transformFirstPersonItem",equipped,f.flag("food")?swing:0f);break;case BLOCK:call("transformFirstPersonItem",equipped,f.flag("block")?swing:0f);call("doBlockTransformations");break;case BOW:call("transformFirstPersonItem",equipped,0f);call("doBowTransformations",partial,p);break;default:call("transformFirstPersonItem",equipped,0f);break;}}
   else{call("doItemUsedTransformations",swing);call("transformFirstPersonItem",equipped,swing);}renderItem(p,item,ItemCameraTransforms.TransformType.FIRST_PERSON);
  }catch(Exception e){f.setEnabled(false);org.apache.logging.log4j.LogManager.getLogger("Zyon").warn("Animacoes restauradas para o padrao.",e);}finally{if(pushed)GlStateManager.popMatrix();GlStateManager.disableRescaleNormal();RenderHelper.disableStandardItemLighting();}
 }
}
