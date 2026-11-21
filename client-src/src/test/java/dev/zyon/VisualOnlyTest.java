package dev.zyon;
import java.lang.reflect.Method;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class VisualOnlyTest {
 @Test public void clientRegistersOnlyLocalVisualFeatures()throws Exception{Zyon client=new Zyon();Method method=Zyon.class.getDeclaredMethod("registerFeatures");method.setAccessible(true);method.invoke(client);Set<String> actual=new HashSet<String>();for(Feature f:client.features)actual.add(f.id);assertEquals(new HashSet<String>(Arrays.asList("hud","keystrokes","crosshair","zoom","theme","badge","selfname","fullbright","itemphysics","nametag","classicpvp","fps","health","mainhand","chestplate","boots","blockoverlay","helmet","leggings","autotext")),actual);}
 @Test public void zoomReleaseRestoresUnmodifiedFov(){assertEquals(30,Visuals.zoomFov(90,3,true),0);assertEquals(90,Visuals.zoomFov(90,3,false),0);assertEquals(5,Visuals.zoomFov(10,6,true),0);}
}
