package dev.zyon;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class Keystrokes {
 private final Minecraft mc=Minecraft.getMinecraft();private final Zyon client;
 private final Deque<Long> left=new ArrayDeque<Long>(),right=new ArrayDeque<Long>();
 public Keystrokes(Zyon client){this.client=client;}
 private Feature f(){return client.feature("keystrokes");}
 public void click(int button){if(button!=0&&button!=1)return;Deque<Long> clicks=button==0?left:right;prune(clicks);if(clicks.size()<200)clicks.addLast(System.nanoTime());}
 private void prune(Deque<Long> clicks){long cutoff=System.nanoTime()-1000000000L;while(!clicks.isEmpty()&&clicks.peekFirst()<cutoff)clicks.removeFirst();}
 private int cps(Deque<Long> clicks){prune(clicks);return clicks.size();}
 private int logicalHeight(){return 47+(f().flag("mouse")?(f().flag("cps")?35:25):0)+(f().flag("jump")?21:0);}
 public float scale(int width,int height){return HudLayout.scale(f().number("scale"),width,height,72,logicalHeight());}
 public int[] bounds(int width,int height){float scale=scale(width,height);int w=(int)Math.ceil(72*scale),h=(int)Math.ceil(logicalHeight()*scale);return new int[]{HudLayout.position(f().number("x"),width,w),HudLayout.position(f().number("y"),height,h),w,h};}
 public void move(int x,int y,int width,int height){int[] b=bounds(width,height);f().setting("x").set(HudLayout.percent(x,width,b[2]));f().setting("y").set(HudLayout.percent(y,height,b[3]));}
 public void reset(){f().setting("x").set(2);f().setting("y").set(35);}
 private boolean down(KeyBinding binding){int code=binding.getKeyCode();return code<0?code+100>=0&&code+100<Mouse.getButtonCount()&&Mouse.isButtonDown(code+100):code>0&&code<Keyboard.KEYBOARD_SIZE&&Keyboard.isKeyDown(code);}
 private int pressedColor(){return new int[]{client.accent(),0xFFF0F1FA,0xFF72A7FF,0xFF72D9A0,0xFFFF83B7}[f().setting("color").integer()];}
 private void box(String label,int x,int y,int w,int h,boolean pressed,String extra){
  int opacity=f().setting("opacity").integer();int bg=pressed?pressedColor():(0x00122538|(opacity<<24));
  Gui.drawRect(x,y,x+w,y+h,bg);Gui.drawRect(x,y+h-1,x+w,y+h,pressed?0xFFFFFFFF:0xFF285271);
  int text=pressed?0xFF03192C:0xFFF0F1FA;
  String name=mc.fontRendererObj.trimStringToWidth(label,w-4);
  mc.fontRendererObj.drawString(name,x+(w-mc.fontRendererObj.getStringWidth(name))/2,y+(extra==null?(h-8)/2:5),text);
  if(extra!=null)mc.fontRendererObj.drawString(extra,x+(w-mc.fontRendererObj.getStringWidth(extra))/2,y+19,text);
 }
 private void key(KeyBinding binding,int x,int y,int w,int h){box(GameSettings.getKeyDisplayString(binding.getKeyCode()),x,y,w,h,down(binding),null);}
 public void draw(int width,int height,boolean preview){
  if(!preview&&!f().enabled())return;int[] b=bounds(width,height);float scale=scale(width,height);
  GL11.glPushMatrix();GL11.glTranslatef(b[0],b[1],0);GL11.glScalef(scale,scale,1);
  try{
   key(mc.gameSettings.keyBindForward,25,0,22,22);key(mc.gameSettings.keyBindLeft,0,25,22,22);key(mc.gameSettings.keyBindBack,25,25,22,22);key(mc.gameSettings.keyBindRight,50,25,22,22);
   int top=50;
   if(f().flag("mouse")){boolean cps=f().flag("cps");int h=cps?32:22;box("LMB",0,top,35,h,Mouse.isButtonDown(0),cps?cps(left)+" CPS":null);box("RMB",37,top,35,h,Mouse.isButtonDown(1),cps?cps(right)+" CPS":null);top+=h+3;}
   if(f().flag("jump"))key(mc.gameSettings.keyBindJump,0,top,72,18);
  }finally{GL11.glPopMatrix();}
 }
}
