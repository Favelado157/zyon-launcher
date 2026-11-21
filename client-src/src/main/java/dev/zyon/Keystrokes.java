package dev.zyon;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
public final class Keystrokes {
 private final Minecraft mc=Minecraft.getMinecraft();private final Zyon client;private final Deque<Long> left=new ArrayDeque<Long>(),right=new ArrayDeque<Long>();
 public Keystrokes(Zyon c){client=c;}private Feature f(){return client.feature("keystrokes");}
 public void click(int b){if(b<0||b>1)return;Deque<Long> q=b==0?left:right;prune(q);if(q.size()<200)q.addLast(System.nanoTime());}
 private void prune(Deque<Long> q){long cut=System.nanoTime()-1000000000L;while(!q.isEmpty()&&q.peekFirst()<cut)q.removeFirst();}private int cps(Deque<Long> q){prune(q);return q.size();}
 public int code(int i){return f().setting("_code"+i).integer();}public int keyX(int i){return f().setting("_kx"+i).integer();}public int keyY(int i){return f().setting("_ky"+i).integer();}
 public boolean visible(int i){return f().flag("_key"+i)&&(code(i)>=0||f().flag("mouse"))&&(code(i)!=Keyboard.KEY_SPACE||f().flag("jump"));}
 public int keyWidth(int i){return code(i)==Keyboard.KEY_SPACE?72:code(i)<0?35:22;}public int keyHeight(int i){return code(i)<0&&f().flag("cps")?32:code(i)==Keyboard.KEY_SPACE?18:22;}
 public int[] size(){int w=1,h=1;for(int i=0;i<8;i++)if(visible(i)){w=Math.max(w,keyX(i)+keyWidth(i));h=Math.max(h,keyY(i)+keyHeight(i));}return new int[]{w,h};}
 public float scale(int w,int h){int[] s=size();return HudLayout.scale(f().number("scale"),w,h,s[0],s[1]);}
 public int[] bounds(int w,int h){int[] s=size();float scale=scale(w,h);int ww=(int)Math.ceil(s[0]*scale),hh=(int)Math.ceil(s[1]*scale);return new int[]{Ui.aligned(f().number("x"),w,ww,f().setting("alignX").integer()),Ui.aligned(f().number("y"),h,hh,f().setting("alignY").integer()),ww,hh};}
 public void move(int x,int y,int w,int h){int[] b=bounds(w,h);f().setting("alignX").set(0);f().setting("alignY").set(0);f().setting("x").set(HudLayout.percent(x,w,b[2]));f().setting("y").set(HudLayout.percent(y,h,b[3]));}
 public void reset(){f().setting("x").set(2);f().setting("y").set(35);}
 public void resetKeys(){int[][] p={{25,0},{0,25},{25,25},{50,25},{0,71},{37,71},{0,50},{0,109}};int[] codes={17,30,31,32,-100,-99,57,42};for(int i=0;i<8;i++){f().setting("_key"+i).set(i<7?1:0);f().setting("_code"+i).set(codes[i]);f().setting("_kx"+i).set(p[i][0]);f().setting("_ky"+i).set(p[i][1]);}}
 private boolean down(int code){return code<0?code+100>=0&&code+100<Mouse.getButtonCount()&&Mouse.isButtonDown(code+100):code>0&&code<Keyboard.KEYBOARD_SIZE&&Keyboard.isKeyDown(code);}
 private int pressed(){return f().flag("customPressed")?f().setting("pressed").rgb():new int[]{client.accent(),0xFFF0F1FA,0xFF72A7FF,0xFF72D9A0,0xFFFF83B7}[f().setting("color").integer()];}
 public void drawKey(int i,int x,int y,boolean preview){int w=keyWidth(i),h=keyHeight(i),code=code(i);boolean active=down(code);int r=f().flag("rounded")?f().setting("radius").integer():0;int text=f().setting("text").rgb();if(f().flag("outline"))Ui.box(x-1,y-1,w+2,h+2,r+1,f().flag("textOutline")?text:f().setting("outlineColor").rgb());Ui.box(x,y,w,h,r,active?pressed():(f().setting("opacity").integer()<<24)|f().setting("background").integer());String label=code==-100?"LMB":code==-99?"RMB":code==Keyboard.KEY_SPACE?"---":GameSettings.getKeyDisplayString(code);label=mc.fontRendererObj.trimStringToWidth(label,w-4);boolean showCps=code<0&&f().flag("cps");mc.fontRendererObj.drawString(label,x+(w-mc.fontRendererObj.getStringWidth(label))/2,y+(showCps?5:(h-8)/2),text);if(showCps){String s=cps(code==-100?left:right)+" CPS";mc.fontRendererObj.drawString(s,x+(w-mc.fontRendererObj.getStringWidth(s))/2,y+19,text);}}
 public void drawAt(int x,int y,boolean preview){for(int i=0;i<8;i++)if(visible(i))drawKey(i,x+keyX(i),y+keyY(i),preview);}
 public void draw(int w,int h,boolean preview){if(!preview&&!f().enabled())return;int[] b=bounds(w,h);float s=scale(w,h);GL11.glPushMatrix();GL11.glTranslatef(b[0],b[1],0);GL11.glScalef(s,s,1);try{drawAt(0,0,preview);}finally{GL11.glPopMatrix();}}
}
