package dev.zyon;
/** Virtual catalog geometry: one source for scrolling, painting and hit testing. */
public final class CatalogLayout {
 private CatalogLayout(){}
 public static int[] card(int index,boolean list,int scroll){return new int[]{70+(list?0:index%2*333),143+index/(list?1:2)*61-scroll,list?656:323,53};}
 public static int rows(int count,boolean list){return (count+(list?0:1))/(list?1:2);}
 public static int clamp(int scroll,int count,boolean list){return Math.max(0,Math.min(scroll,Math.max(0,rows(count,list)*61-269)));}
 public static int hit(int x,int y,boolean list,int scroll,int count){if(y<140||y>=408)return -1;for(int i=0;i<count;i++){int[] c=card(i,list,clamp(scroll,count,list));if(Ui.hit(x,y,c[0],c[1],c[2],c[3]))return i;}return -1;}
}
