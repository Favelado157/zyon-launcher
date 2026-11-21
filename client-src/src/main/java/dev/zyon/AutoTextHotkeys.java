package dev.zyon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
public final class AutoTextHotkeys {
 private final Zyon client;private final Minecraft mc=Minecraft.getMinecraft();private final AutoTextTrigger trigger=new AutoTextTrigger();
 public AutoTextHotkeys(Zyon c){client=c;}
 public static boolean reserved(Zyon client,int key){
  if(key<=0||key>=Keyboard.KEYBOARD_SIZE||key==Keyboard.KEY_ESCAPE||key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER||key==Keyboard.KEY_TAB||key==Keyboard.KEY_F1||key==Keyboard.KEY_F2||key==Keyboard.KEY_F3||key==Keyboard.KEY_F4||key==Keyboard.KEY_F5||key==Keyboard.KEY_F11||key==Keyboard.KEY_DELETE||key==Keyboard.KEY_BACK)return true;
  Minecraft mc=Minecraft.getMinecraft();for(KeyBinding binding:mc.gameSettings.keyBindings)if(binding.getKeyCode()==key)return true;
  for(Feature feature:client.features)if(feature.key==key)return true;return false;
 }
 public boolean handle(int key,boolean down,boolean repeat){boolean active=mc.thePlayer!=null&&mc.theWorld!=null&&mc.currentScreen==null&&mc.inGameHasFocus&&Display.isActive()&&client.feature("autotext").enabled();int slot=trigger.input(key,down,repeat,active,client.autoText);if(slot<0||reserved(client,key))return false;new Sender().sendChatMessage(client.autoText.message(slot));return true;}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){if(event.phase==TickEvent.Phase.END)for(int i=1;i<Keyboard.KEYBOARD_SIZE;i++)if(!Keyboard.isKeyDown(i))trigger.released(i);}
 private static final class Sender extends GuiScreen {Sender(){mc=Minecraft.getMinecraft();}}
}
