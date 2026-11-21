package dev.zyon;
import org.junit.Test;
import static org.junit.Assert.*;
public class CatalogLayoutTest {
 @Test public void bothColumnsRemainSelectableAfterScrolling(){for(boolean list:new boolean[]{false,true}){int scroll=CatalogLayout.clamp(99999,17,list);boolean lastVisible=false;for(int i=0;i<17;i++){int[] b=CatalogLayout.card(i,list,scroll);int y=Math.max(140,b[1]+5);if(y<b[1]+b[3]&&y<408){assertEquals(i,CatalogLayout.hit(b[0]+10,y,list,scroll,17));if(i==16)lastVisible=true;}}assertTrue(lastVisible);}}
 @Test public void clippedRowsAndCardGapsCannotReceiveClicks(){assertEquals(-1,CatalogLayout.hit(75,139,false,0,17));assertEquals(-1,CatalogLayout.hit(75,408,false,0,17));assertEquals(-1,CatalogLayout.hit(398,165,false,0,17));assertEquals(-1,CatalogLayout.hit(75,198,false,0,17));}
 @Test public void searchAndViewChangesClampStaleScroll(){assertEquals(0,CatalogLayout.clamp(999,1,false));assertEquals(0,CatalogLayout.clamp(-30,17,true));assertEquals(280,CatalogLayout.clamp(9999,17,false));assertEquals(768,CatalogLayout.clamp(9999,17,true));}
}
