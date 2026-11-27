package dev.zyon;
import org.junit.Test;import static org.junit.Assert.*;import dev.zyon.platform.*;import dev.zyon.platform.event.*;
public class StandalonePlatformTest {
 public static final class Listener {int pre,post;@SubscribeEvent public void before(GuiOpenEvent e){pre++;e.setCanceled(true);}@SubscribeEvent public void after(TickEvent.ClientTickEvent e){post++;}}
 @Test public void platformDoesNotLoadLegacyRuntime(){assertNull(Hooks.class.getClassLoader().getResource("net/minecraftforge/common/MinecraftForge.class"));assertNull(Hooks.class.getClassLoader().getResource("net/minecraft/launchwrapper/Launch.class"));}
 @Test public void repeatedRegistrationDoesNotDuplicateCallbacks(){Platform.Bus bus=new Platform.Bus();Listener listener=new Listener();bus.register(listener);bus.register(listener);assertTrue(bus.post(new GuiOpenEvent(null)).isCanceled());assertEquals(1,listener.pre);bus.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));assertEquals(1,listener.post);}
 @Test public void reflectionSupportsAliasesAndInheritedMembers(){assertEquals("value",ReflectionHelper.findField(Child.class,"missing","value").getName());assertEquals("test",ReflectionHelper.findMethod(Child.class,null,new String[]{"missing","test"},int.class).getName());}
 @Test public void worldOverlaysRunBeforeHandResetsCameraAndDepth()throws Exception{
  org.objectweb.asm.tree.ClassNode c=new org.objectweb.asm.tree.ClassNode();try(java.io.InputStream in=getClass().getResourceAsStream("/net/minecraft/client/renderer/EntityRenderer.class")){new org.objectweb.asm.ClassReader(in).accept(c,0);}
  int hook=-1,hand=-1,lastClear=-1,count=0;for(org.objectweb.asm.tree.MethodNode m:c.methods)if(m.name.equals("renderWorldPass")){int index=0;for(org.objectweb.asm.tree.AbstractInsnNode i:m.instructions.toArray()){if(i instanceof org.objectweb.asm.tree.MethodInsnNode){org.objectweb.asm.tree.MethodInsnNode call=(org.objectweb.asm.tree.MethodInsnNode)i;if(call.owner.equals("dev/zyon/platform/Hooks")&&call.name.equals("worldLast")){hook=index;count++;}if(call.owner.equals("net/minecraft/client/renderer/GlStateManager")&&call.name.equals("clear"))lastClear=index;if(call.name.equals("renderHand")){hand=index;break;}}index++;}}
  assertEquals(1,count);assertTrue("Overlay must preserve world projection and depth",hook>=0&&hook<lastClear&&lastClear<hand);
 }
 static class Parent {private int value;private void test(int input){}}static class Child extends Parent{}
 @Test public void vanillaCallbackNamesRemainCompatible()throws Exception{assertNotNull(net.minecraft.client.gui.GuiScreen.class.getDeclaredMethod("confirmClicked",boolean.class,int.class));assertNotNull(net.minecraft.client.multiplayer.WorldClient.class.getMethod("getBlockState",net.minecraft.util.BlockPos.class));assertNotNull(net.minecraft.client.entity.EntityPlayerSP.class.getMethod("getName"));}
}
