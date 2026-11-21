package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Independent local widgets; positions use available space, so resizing keeps them on screen. */
public final class Widgets {
 private final Zyon client;private final Minecraft mc=Minecraft.getMinecraft();
 public Widgets(Zyon c){client=c;}
 public int[] bounds(Feature f,int width,int height){int logicalW=f.id.equals("fps")?105:98,logicalH=f.id.equals("fps")?(int)(18*f.number("lineHeight")/100):23;float s=HudLayout.scale(f.number("scale"),width,height,logicalW,logicalH);int w=(int)Math.ceil(logicalW*s),h=(int)Math.ceil(logicalH*s);return new int[]{Ui.aligned(f.number("x"),width,w,f.setting("alignX").integer()),Ui.aligned(f.number("y"),height,h,f.setting("alignY").integer()),w,h};}
 public void move(Feature f,int x,int y,int width,int height){int[] b=bounds(f,width,height);f.setting("alignX").set(0);f.setting("alignY").set(0);f.setting("x").set(HudLayout.percent(x,width,b[2]));f.setting("y").set(HudLayout.percent(y,height,b[3]));}
 public void draw(Feature f,int width,int height,boolean preview){int[] b=bounds(f,width,height);GlStateManager.pushMatrix();GlStateManager.translate(b[0],b[1],0);float s=(float)(b[2]/(f.id.equals("fps")?105.0:98.0));GlStateManager.scale(s,s,1);try{content(client,f,0,0,preview);}finally{GlStateManager.popMatrix();}}
 public static void preview(Zyon c,Feature f,int x,int y){content(c,f,x,y,true);}
 private static void part(Minecraft mc,String s,int[] x,int y,int color){mc.fontRendererObj.drawStringWithShadow(s,x[0],y,color);x[0]+=mc.fontRendererObj.getStringWidth(s);}
 private static void content(Zyon c,Feature f,int x,int y,boolean preview){Minecraft mc=Minecraft.getMinecraft();if(f.id.equals("fps")){String value=Integer.toString(preview?144:Minecraft.getDebugFPS());int format=f.setting("format").integer();int[] xx={x};if(format==1)part(mc,"[",xx,y,f.setting("bracketColor").rgb());if(format<2)part(mc,"FPS: ",xx,y,f.setting("labelColor").rgb());part(mc,value,xx,y,f.setting("valueColor").rgb());if(format==1)part(mc,"]",xx,y,f.setting("bracketColor").rgb());if(format==2)part(mc," FPS",xx,y,f.setting("labelColor").rgb());return;}
  ItemStack stack=null;if(!preview&&mc.thePlayer!=null)stack=f.id.equals("mainhand")?mc.thePlayer.getHeldItem():mc.thePlayer.inventory.armorInventory[armorSlot(f.id)];if(preview)stack=new ItemStack(f.id.equals("mainhand")?Items.diamond_sword:f.id.equals("helmet")?Items.diamond_helmet:f.id.equals("chestplate")?Items.diamond_chestplate:f.id.equals("leggings")?Items.diamond_leggings:Items.diamond_boots,1,100);
  if(stack==null)return;GlStateManager.pushMatrix();GlStateManager.enableDepth();RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(stack,x,y);RenderHelper.disableStandardItemLighting();GlStateManager.disableDepth();GlStateManager.popMatrix();
  int mode=f.setting("durability").integer();if(mode==3||!stack.isItemStackDamageable())return;double fraction=durability(stack.getMaxDamage(),stack.getItemDamage());int color=f.setting("durabilityColor").integer()==1?java.awt.Color.HSBtoRGB((float)fraction/3,1,1):0xFFFFFFFF;
  if(mode==0){Gui.drawRect(x+21,y+6,x+85,y+12,0xB0000000);Gui.drawRect(x+22,y+7,x+22+(int)(62*fraction),y+11,color);}else mc.fontRendererObj.drawStringWithShadow(mode==1?Math.round(fraction*100)+"%":Integer.toString(Math.max(0,stack.getMaxDamage()-stack.getItemDamage())),x+21,y+4,color);
 }
 public static int armorSlot(String id){if(id.equals("helmet"))return 3;if(id.equals("chestplate"))return 2;if(id.equals("leggings"))return 1;if(id.equals("boots"))return 0;throw new IllegalArgumentException("Equipamento desconhecido: "+id);}
 public static double durability(int max,int damage){return max<=0?0:Math.max(0,Math.min(1,(max-damage)/(double)max));}
}
