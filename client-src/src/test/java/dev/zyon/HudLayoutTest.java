package dev.zyon;
import org.junit.Test;
import static org.junit.Assert.*;
public class HudLayoutTest {
 @Test public void draggingToEdgesStaysOnScreen(){assertEquals(0,HudLayout.position(-10,320,72));assertEquals(248,HudLayout.position(120,320,72));assertEquals(0,HudLayout.percent(-20,320,72),0);assertEquals(100,HudLayout.percent(999,320,72),0);}
 @Test public void storedPositionTracksResizing(){double saved=HudLayout.percent(124,320,72);assertEquals(50,saved,0);assertEquals(284,HudLayout.position(saved,640,72));}
 @Test public void oversizeWidgetDoesNotDivideByZero(){assertEquals(0,HudLayout.percent(10,40,72),0);assertEquals(0,HudLayout.position(80,40,72));}
 @Test public void scaleFitsSmallScreens(){float scale=HudLayout.scale(2,100,80,72,103);assertTrue(scale*72<=92.001);assertTrue(scale*103<=72.001);assertEquals(.5,HudLayout.scale(.5,800,600,72,103),.00001);}
}
