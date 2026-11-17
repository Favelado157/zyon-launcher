package dev.zyon;

/** Resolution-independent widget coordinates, stored as percentages of the usable area. */
public final class HudLayout {
 private HudLayout() {}
 public static int position(double percent,int screen,int widget){
  return (int)Math.round(Math.max(0,screen-widget)*Math.max(0,Math.min(100,percent))/100.0);
 }
 public static double percent(int pixel,int screen,int widget){
  int available=Math.max(0,screen-widget);
  return available==0?0:100.0*Math.max(0,Math.min(available,pixel))/available;
 }
 public static float scale(double requested,int screenWidth,int screenHeight,int width,int height){
  return (float)Math.max(.01,Math.min(requested,Math.min(Math.max(1,screenWidth-8)/(double)width,Math.max(1,screenHeight-8)/(double)height)));
 }
}
