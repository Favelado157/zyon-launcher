package dev.zyon;
import net.minecraft.client.gui.Gui;
public final class Ui {
 private Ui(){}
 public static void box(int x,int y,int w,int h,int radius,int color){int r=Math.min(Math.min(w,h)/2,Math.max(0,radius));Gui.drawRect(x+r,y,x+w-r,y+h,color);for(int i=0;i<r;i++){int inset=(int)Math.ceil(r-Math.sqrt(r*r-(r-i-.5)*(r-i-.5)));Gui.drawRect(x+inset,y+i,x+w-inset,y+i+1,color);Gui.drawRect(x+inset,y+h-i-1,x+w-inset,y+h-i,color);}Gui.drawRect(x,y+r,x+w,y+h-r,color);}
 public static boolean hit(int mx,int my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
 public static int aligned(double percent,int extent,int size,int alignment){if(alignment==1)return 4;if(alignment==2)return Math.max(0,(extent-size)/2);if(alignment==3)return Math.max(0,extent-size-4);return HudLayout.position(percent,extent,size);}
}
