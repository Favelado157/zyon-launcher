package dev.zyon;
import org.junit.Test;import static org.junit.Assert.*;import dev.zyon.platform.*;import dev.zyon.platform.event.*;
public class StandalonePlatformTest {
 public static final class Listener {int pre,post;@SubscribeEvent public void before(GuiOpenEvent e){pre++;e.setCanceled(true);}@SubscribeEvent public void after(TickEvent.ClientTickEvent e){post++;}}
 @Test public void platformDoesNotLoadLegacyRuntime(){assertNull(Hooks.class.getClassLoader().getResource("net/minecraftforge/common/MinecraftForge.class"));assertNull(Hooks.class.getClassLoader().getResource("net/minecraft/launchwrapper/Launch.class"));}
 @Test public void repeatedRegistrationDoesNotDuplicateCallbacks(){Platform.Bus bus=new Platform.Bus();Listener listener=new Listener();bus.register(listener);bus.register(listener);assertTrue(bus.post(new GuiOpenEvent(null)).isCanceled());assertEquals(1,listener.pre);bus.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));assertEquals(1,listener.post);}
 @Test public void reflectionSupportsAliasesAndInheritedMembers(){assertEquals("value",ReflectionHelper.findField(Child.class,"missing","value").getName());assertEquals("test",ReflectionHelper.findMethod(Child.class,null,new String[]{"missing","test"},int.class).getName());}
 static class Parent {private int value;private void test(int input){}}static class Child extends Parent{}
 @Test public void vanillaCallbackNamesRemainCompatible()throws Exception{assertNotNull(net.minecraft.client.gui.GuiScreen.class.getDeclaredMethod("confirmClicked",boolean.class,int.class));assertNotNull(net.minecraft.client.multiplayer.WorldClient.class.getMethod("getBlockState",net.minecraft.util.BlockPos.class));assertNotNull(net.minecraft.client.entity.EntityPlayerSP.class.getMethod("getName"));}
}
