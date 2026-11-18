package dev.zyon;
import org.junit.Test;
import static org.junit.Assert.*;
public class ClickLayoutTest {
 @Test public void helpAndToggleHaveSeparateTargets(){for(int row=0;row<5;row++){int top=ClickLayout.LIST_Y+row*ClickLayout.ROW;assertTrue(ClickLayout.helpHit(ClickLayout.HELP_X+9,top+25,top));assertFalse(ClickLayout.toggleHit(ClickLayout.HELP_X+9,top+25,top));assertTrue(ClickLayout.toggleHit(ClickLayout.TOGGLE_X+15,top+25,top));assertFalse(ClickLayout.helpHit(ClickLayout.TOGGLE_X+15,top+25,top));}}
 @Test public void clippedRowsAndSpacingCannotBeClicked(){int x=ClickLayout.HELP_X;assertEquals(-1,ClickLayout.moduleIndex(x,ClickLayout.LIST_Y-1,0,20));assertEquals(-1,ClickLayout.moduleIndex(x,ClickLayout.LIST_Y+ClickLayout.LIST_H,0,20));assertEquals(-1,ClickLayout.moduleIndex(x,ClickLayout.LIST_Y+ClickLayout.CARD_H,0,20));assertEquals(1,ClickLayout.moduleIndex(x,ClickLayout.LIST_Y,58,20));assertEquals(-1,ClickLayout.moduleIndex(x,ClickLayout.LIST_Y,0,0));assertEquals(-1,ClickLayout.moduleIndex(ClickLayout.LIST_X-1,ClickLayout.LIST_Y,0,20));}
 @Test public void tooltipsRemainInsideTheMenuAtEveryCorner(){for(int x:new int[]{0,350,799})for(int y:new int[]{0,230,463}){int[] box=ClickLayout.tooltip(x,y,300,196);assertTrue(box[0]>=10);assertTrue(box[1]>=10);assertTrue(box[0]+300<=ClickLayout.WIDTH-10);assertTrue(box[1]+196<=ClickLayout.HEIGHT-10);}}
 @Test public void scalingFitsSmallAndLargeViewports(){for(int[] viewport:new int[][]{{320,240},{427,240},{854,480},{1920,1080}}){float scale=ClickLayout.scale(viewport[0],viewport[1]);assertTrue(scale*ClickLayout.WIDTH<=viewport[0]-15.99);assertTrue(scale*ClickLayout.HEIGHT<=viewport[1]-15.99);}}
 @Test public void scrollingCannotOvershootContentOrBecomeNegative(){assertEquals(0,ClickLayout.clampScroll(-50,20,58,8,308));assertEquals(844,ClickLayout.clampScroll(9999,20,58,8,308));assertEquals(0,ClickLayout.clampScroll(20,0,58,8,308));assertEquals(0,ClickLayout.clampScroll(20,5,58,8,308));}
}
